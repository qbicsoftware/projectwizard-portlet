==========
Changelog
==========

This project adheres to `Semantic Versioning <https://semver.org/>`_.

1.12.3 (2021-11-13)
-------------------

**Added**

**Fixed**

**Dependencies**

* org.apache.logging.log4j 2.13.2 -> 2.15.0 (addresses CVE-2021-44228)

**Deprecated**


1.12.2 (2021-11-02)
-------------------

**Added**

**Fixed**

**Dependencies**

* `com.vaadin:7.7.17` -> `7.7.28` (addresses CVE-2021-37714)

**Deprecated**


1.12.1 (2021-06-11)
-------------------

**Added**

**Fixed**

* Correctly replace selected vocabulary values for Cell Lysis in metabolomics import (`#32 <https://github.com/qbicsoftware/projectwizard-portlet/pull/32>`_)

**Dependencies**

**Deprecated**


1.12.0 (2021-06-07)
-------------------

**Added**

* Uses new database structure for persons: table "persons" is replaced by table "person" (`#29 <https://github.com/qbicsoftware/projectwizard-portlet/pull/29>`_)

**Fixed**

**Dependencies**

**Deprecated**


1.11.2 (2021-05-31)
-------------------

**Added**

**Fixed**

* Existing experiments are now correctly fetched when using the wizard process

**Dependencies**

**Deprecated**


1.11.1 (2021-05-27)
-------------------

**Added**

* Unit tests for adding barcodes to different import formats

**Fixed**

* Barcodes are once again correctly added to the proteomics import format

**Dependencies**

**Deprecated**


1.11.0 (2021-05-20)
-------------------

**Added**

* Metabolomics import format
* "QBiC" format has been renamed to openBIS-based format
* "Standard" format has been renamed to "Standard QBiC format"

**Fixed**

* Help/Examples symbols for import formats are now places directly next to the respective options
* When importing multiple different formats, the project and experimental design are now correctly reset

**Dependencies**

**Deprecated**


1.10.1 (2021-02-02)
-------------------

**Added**

**Fixed**

* Use new experimental design lib, fixing handling of Peptide Cleanup (PTX import)
* Fix: when replacing user inputs with vocabulary values in the uploaded file, special characters like brackets are now handled
* Fix: importing into an existing project would sometimes fail to create new barcodes

**Dependencies**

**Deprecated**


1.10.0 (2021-02-02)
-------------------

**Added**

* Created the project using Qube

**Fixed**

**Dependencies**

**Deprecated**
