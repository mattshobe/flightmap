#!/bin/bash

ant || exit

rm ../data/aviation.db

java -cp build/classes:lib/commons-lang-2.5.jar:lib/sqlitejdbc-v056.jar:../common/release/flightmap-common-0.01.jar com.google.flightmap.parsing.AviationMasterRecordParser ../data/NfdcFacilities.xls ../data/NfdcRunways.xls ../data/iata2icao.txt ../data/aviation.db

java -cp lib/sqlitejdbc-v056.jar:build/classes/ com.google.flightmap.parsing.AfdCommParser ../data/afd.txt ../data/iata2icao.txt ../data/aviation.db
