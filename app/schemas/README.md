# Migration fixture provenance

Schemas 48–67 under `com.smouldering_durtles.wk.db.AppDatabase` were recovered
byte-for-byte from commit `b36c696`, from
`app/schemas/com.the_tinkering.wk.db.AppDatabase/`. Only their containing directory
changed to match the current Room database class. The schema contents and identity
hashes were not regenerated or edited. Schema 68 was already present.

These are historical test inputs, not a migration of the application database.
