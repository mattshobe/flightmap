#!/bin/bash

ant || exit

rm ../data/aviation.db

java -cp build/classes:lib/commons-lang-2.5.jar:lib/sqlitejdbc-v056.jar:../common/release/flightmap-common-0.01.jar com.google.flightmap.parser.AviationMasterRecordParser ../data/NfdcFacilities.xls ../data/NfdcRunways.xls ../data/iata2icao.txt ../data/aviation.db
