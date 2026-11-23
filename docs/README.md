# Project documents

The corrected MySQL schema is the database contract. For an existing database, apply [the dietary type migration](migrations/001_add_menu_dietary_type.sql) once; do not run it after creating a new database from the updated schema. Existing dishes remain `UNSPECIFIED` until an administrator classifies them.
