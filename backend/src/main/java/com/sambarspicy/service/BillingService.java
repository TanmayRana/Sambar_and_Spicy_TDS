package com.sambarspicy.service;

import com.sambarspicy.dto.BillingDtos.*;
import com.sambarspicy.enums.*;
import com.sambarspicy.exception.ConflictException;
import com.sambarspicy.exception.ResourceNotFoundException;
import com.sambarspicy.model.*;
import com.sambarspicy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingService {
    private final BillRepository bills;
    private final BillCounterRepository counters;
    private final MenuItemRepository menu;
    private final CategoryRepository categories;
    private final StoreSettingRepository stores;
    private final UserRepository users;
    private final AuditLogRepository audits;

    public BillingService(BillRepository bills, BillCounterRepository counters, MenuItemRepository menu,
                          CategoryRepository categories, StoreSettingRepository stores,
                          UserRepository users, AuditLogRepository audits) {
        this.bills=bills; this.counters=counters; this.menu=menu; this.categories=categories;
        this.stores=stores; this.users=users; this.audits=audits;
    }

    private StoreSetting lockStore() {
        return stores.lockSingleton().orElseThrow(() -> new ResourceNotFoundException("Store settings not found"));
    }
    private User actor(String username) {
        return users.findByUsername(username).filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found"));
    }
    private static LocalDateTime utcNow() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static LocalDate businessDate() { return LocalDate.now(ZoneId.of("Asia/Kolkata")); }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Bill lockedHeld(Long id) {
        Bill bill = bills.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        if (bill.getStatus() != BillStatus.HELD) throw new ConflictException("Only held bills can be changed");
        return bill;
    }

    @Transactional(readOnly = true)
    public List<Detail> held() {
        return bills.findByStatusOrderByCreatedAtAsc(BillStatus.HELD).stream().map(this::detail).toList();
    }
    @Transactional(readOnly = true)
    public Detail get(Long id) {
        return detail(bills.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bill not found")));
    }
    @Transactional(readOnly = true)
    public List<Detail> list(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("Invalid date range");
        return bills.findByBusinessDateBetweenOrderByCreatedAtDesc(
                from == null ? LocalDate.of(1000,1,1) : from, to == null ? LocalDate.of(9999,12,31) : to)
                .stream().map(this::detail).toList();
    }
    @Transactional
    public Detail hold(BillRequest request, String username) {
        lockStore();
        User user = actor(username);
        Bill bill = request.billId() == null ? new Bill(businessDate(), user) : lockedHeld(request.billId());
        replaceItems(bill, request.items(), false);
        bill.setHoldLabel(clean(request.holdLabel())); bill.setNote(clean(request.note())); bill.setHeldAt(utcNow());
        bills.saveAndFlush(bill);
        audit(user, "HOLD", bill);
        return detail(bill);
    }
    @Transactional
    public Detail cancel(Long id, String reason, String username) {
        lockStore();
        User user = actor(username); Bill bill = lockedHeld(id);
        bill.setStatus(BillStatus.CANCELLED); bill.setCancelledBy(user);
        bill.setCancelReason(reason.trim()); bill.setCancelledAt(utcNow());
        bills.saveAndFlush(bill); audit(user, "CANCEL", bill);
        return detail(bill);
    }
    @Transactional
    public Detail checkout(CheckoutRequest request, String keyHeader, String username) {
        if (!request.checkoutKey().equals(keyHeader)) throw new IllegalArgumentException("Checkout key header must match request");
        try { if (!UUID.fromString(request.checkoutKey()).toString().equals(request.checkoutKey())) throw new IllegalArgumentException(); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Checkout key must be a UUID"); }
        if (request.referenceNumber() != null && request.referenceNumber().matches(".*(?:\\d[ -]?){13,}.*"))
            throw new IllegalArgumentException("Reference must not contain a full card number");
        StoreSetting store = lockStore();
        User user = actor(username);
        Optional<Bill> previous = bills.findByCheckoutKey(request.checkoutKey());
        if (previous.isPresent()) {
            Bill existing = previous.get();
            if (!matches(existing, request)) throw new ConflictException("Checkout key already belongs to a different payment");
            return detail(existing);
        }
        Bill bill = request.billId() == null ? new Bill(businessDate(), user) : lockedHeld(request.billId());
        replaceItems(bill, request.items(), true);
        BigDecimal unrounded = bill.getTotalAmount();
        BigDecimal total = Boolean.TRUE.equals(request.roundOff()) ? unrounded.setScale(0, RoundingMode.HALF_UP).setScale(2) : unrounded;
        if (total.compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new IllegalArgumentException("Bill total exceeds the supported amount");
        bill.setRoundOffAmount(total.subtract(unrounded));
        bill.setTotalAmount(total);
        BigDecimal tendered = request.tenderedAmount();
        if (total.signum() <= 0 || tendered.compareTo(total) < 0 ||
                (request.paymentMethod() != PaymentMethod.CASH && tendered.compareTo(total) != 0))
            throw new IllegalArgumentException("Invalid payment amount");
        LocalDate date = businessDate();
        BillCounter counter = counters.findByBusinessDateForUpdate(date).orElseGet(() -> new BillCounter(date, 0));
        if (counter.getLastSequence() == Integer.MAX_VALUE) throw new ConflictException("Daily bill sequence exhausted");
        counter.setLastSequence(counter.getLastSequence()+1);
        counters.saveAndFlush(counter);
        bill.setBusinessDate(date);
        bill.setBillNumber("SNS-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format(Locale.ROOT, "%04d", counter.getLastSequence()));
        bill.setCheckoutKey(request.checkoutKey()); bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(utcNow()); bill.setPaidBy(user); bill.setHoldLabel(clean(request.holdLabel()));
        bill.setNote(clean(request.note())); bill.setStoreNameSnapshot(store.getStoreName());
        bill.setStoreAddressSnapshot(store.getAddress()); bill.setStorePhoneSnapshot(store.getPhone());
        BillPayment payment = new BillPayment(bill, request.paymentMethod(), total, tendered, user);
        payment.setReferenceNumber(clean(request.referenceNumber())); payment.setRecordedAt(utcNow());
        bill.setPayment(payment);
        bills.saveAndFlush(bill); audit(user, "CHECKOUT", bill);
        return detail(bill);
    }

    @Transactional
    public void reprint(Long id, String username) {
        User user = actor(username);
        Bill bill = bills.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        if (bill.getStatus() != BillStatus.PAID) throw new ConflictException("Only paid bills can be reprinted");
        audit(user, "REPRINT", bill);
    }
    private void audit(User actor, String action, Bill bill) {
        audits.save(new AuditLog(actor, action, "bill", String.valueOf(bill.getId()), null, null, null));
    }
    private void replaceItems(Bill bill, List<LineRequest> requested, boolean reprice) {
        Set<String> seen = new HashSet<>();
        Map<String, BillItem> existing = bill.getBillItems().stream().collect(Collectors.toMap(
                line -> line.getMenuItem().getId() + ":" + line.getPortion(), line -> line));
        List<BillItem> replacement = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO, discount = BigDecimal.ZERO;
        int count = 0;
        List<LineRequest> ordered = new ArrayList<>(requested);
        ordered.sort((left, right) -> {
            int itemOrder = Long.compare(left.menuItemId(), right.menuItemId());
            if (itemOrder != 0) return itemOrder;
            PortionSize leftPortion = left.portion() == null ? PortionSize.FULL : left.portion();
            PortionSize rightPortion = right.portion() == null ? PortionSize.FULL : right.portion();
            return leftPortion.name().compareTo(rightPortion.name());
        });
        for (LineRequest input : ordered) {
            PortionSize portion = input.portion() == null ? PortionSize.FULL : input.portion();
            String key = input.menuItemId() + ":" + portion;
            if (!seen.add(key)) throw new IllegalArgumentException("Duplicate dish and portion");
            MenuItem item = menu.findByIdForBilling(input.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
            Category category = categories.findByIdForBilling(item.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            if (!Boolean.TRUE.equals(item.getIsAvailable()) || !Boolean.TRUE.equals(category.getIsActive()) ||
                    (portion == PortionSize.HALF && (!Boolean.TRUE.equals(item.getHasHalfPortion()) || item.getHalfSellingPrice() == null)))
                throw new ConflictException("Dish or portion is no longer available: " + item.getName());
            BillItem saved = reprice ? null : existing.get(key);
            BigDecimal price = saved != null ? saved.getUnitPrice() : portion == PortionSize.HALF ? item.getHalfSellingPrice() : item.getSellingPrice();
            if (reprice && (input.expectedUnitPrice() == null || price.compareTo(input.expectedUnitPrice()) != 0))
                throw new ConflictException("Dish price changed. Remove and re-add the item before checkout: " + item.getName());
            BigDecimal cost = saved != null ? saved.getEstimatedUnitCostSnapshot() : portion == PortionSize.HALF ? item.getHalfEstimatedUnitCost() : item.getEstimatedUnitCost();
            BigDecimal lineDiscount = input.lineDiscountAmount() == null ? BigDecimal.ZERO : input.lineDiscountAmount();
            BigDecimal gross = price.multiply(BigDecimal.valueOf(input.quantity()));
            if (lineDiscount.signum() < 0 || lineDiscount.scale() > 2 || lineDiscount.compareTo(gross) > 0)
                throw new IllegalArgumentException("Invalid line discount");
            subtotal = subtotal.add(gross); discount = discount.add(lineDiscount);
            count = Math.addExact(count, input.quantity());
            BillItem line = new BillItem(bill, item, saved != null ? saved.getItemNameSnapshot() : item.getName(), input.quantity(), price, portion);
            line.setEstimatedUnitCostSnapshot(cost); line.setLineDiscountAmount(lineDiscount); line.setCreatedAt(utcNow());
            replacement.add(line);
        }
        if (subtotal.compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new IllegalArgumentException("Bill total exceeds the supported amount");
        if (bill.getId() != null) { bill.getBillItems().clear(); bills.saveAndFlush(bill); }
        replacement.forEach(bill::addBillItem);
        bill.setItemCount(count); bill.setSubtotalAmount(subtotal.setScale(2, RoundingMode.UNNECESSARY));
        bill.setDiscountAmount(discount.setScale(2, RoundingMode.UNNECESSARY));
        bill.setRoundOffAmount(BigDecimal.ZERO.setScale(2));
        bill.setTotalAmount(subtotal.subtract(discount).setScale(2, RoundingMode.UNNECESSARY));
    }
    private boolean matches(Bill bill, CheckoutRequest request) {
        BigDecimal net = bill.getSubtotalAmount().subtract(bill.getDiscountAmount());
        BigDecimal expectedRoundOff = Boolean.TRUE.equals(request.roundOff()) ? net.setScale(0, RoundingMode.HALF_UP).setScale(2).subtract(net) : BigDecimal.ZERO;
        if (bill.getStatus() != BillStatus.PAID || bill.getPayment() == null ||
                bill.getRoundOffAmount().compareTo(expectedRoundOff) != 0 ||
                (request.billId() != null && !request.billId().equals(bill.getId())) ||
                bill.getPayment().getPaymentMethod() != request.paymentMethod() ||
                bill.getPayment().getTenderedAmount().compareTo(request.tenderedAmount()) != 0 ||
                !Objects.equals(bill.getPayment().getReferenceNumber(), clean(request.referenceNumber())) ||
                !Objects.equals(bill.getNote(), clean(request.note())) ||
                !Objects.equals(bill.getHoldLabel(), clean(request.holdLabel())) ||
                bill.getBillItems().size() != request.items().size()) return false;
        Map<String, BillItem> lines = bill.getBillItems().stream().collect(Collectors.toMap(
                x -> x.getMenuItem().getId() + ":" + x.getPortion(), x -> x));
        Set<String> seen = new HashSet<>();
        for (LineRequest requested : request.items()) {
            String key = requested.menuItemId() + ":" + (requested.portion() == null ? PortionSize.FULL : requested.portion());
            BillItem line = lines.get(key);
            if (!seen.add(key) || line == null || !line.getQuantity().equals(requested.quantity()) ||
                    requested.expectedUnitPrice() == null || line.getUnitPrice().compareTo(requested.expectedUnitPrice()) != 0 ||
                    line.getLineDiscountAmount().compareTo(requested.lineDiscountAmount() == null ? BigDecimal.ZERO : requested.lineDiscountAmount()) != 0)
                return false;
        }
        return true;
    }
    private Detail detail(Bill bill) {
        List<Line> lines = bill.getBillItems().stream().map(x -> new Line(x.getId(), bill.getId(), x.getMenuItem().getId(),
                x.getPortion(), x.getItemNameSnapshot(), x.getQuantity(), x.getUnitPrice(), x.getEstimatedUnitCostSnapshot(),
                x.getLineDiscountAmount(), x.getLineTotal(), x.getLineNetTotal(), x.getCreatedAt())).toList();
        BillPayment p = bill.getPayment();
        Payment payment = p == null ? null : new Payment(p.getId(), bill.getId(), p.getPaymentMethod(), p.getAmount(),
                p.getTenderedAmount(), p.getChangeAmount(), p.getReferenceNumber(), p.getRecordedBy().getId(), p.getRecordedAt());
        return new Detail(bill.getId(), bill.getBillNumber(), bill.getCheckoutKey(), bill.getBusinessDate(), bill.getStatus().name(),
                bill.getHoldLabel(), bill.getItemCount(), bill.getSubtotalAmount(), bill.getDiscountAmount(), bill.getRoundOffAmount(), bill.getTotalAmount(),
                bill.getNote(), bill.getStoreNameSnapshot(), bill.getStoreAddressSnapshot(), bill.getStorePhoneSnapshot(),
                bill.getCreatedBy().getId(), bill.getPaidBy() == null ? null : bill.getPaidBy().getId(),
                bill.getCancelledBy() == null ? null : bill.getCancelledBy().getId(), bill.getCancelReason(), bill.getCreatedAt(),
                bill.getHeldAt(), bill.getPaidAt(), bill.getCancelledAt(), bill.getUpdatedAt(), lines, payment,
                bill.getPaidBy() == null ? bill.getCreatedBy().getFullName() : bill.getPaidBy().getFullName());
    }
}
