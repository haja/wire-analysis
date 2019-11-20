/*
 * Copyright (c) 2020 Harald Jagenteufel.
 *
 * This file is part of wire-analysis.
 *
 *     wire-analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     wire-analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with wire-analysis.  If not, see <https://www.gnu.org/licenses/>.
 */

package at.sbaresearch.mqtt4android.analysis.statistics;

import at.sbaresearch.mqtt4android.analysis.statistics.Statistics.MessageStats;
import com.google.common.base.Charsets;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.SortedMap;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticsExporter {

  String statisticsPath;
  Executor executor;
  CSVPrinter printer;
  DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public StatisticsExporter(@org.springframework.beans.factory.annotation.Value(
      "${statistics.export.base}") String statisticsExportBase) throws IOException {

    val timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    statisticsPath = statisticsExportBase.replace("$date", timestamp);
    log.info("statistics export path: {}", statisticsPath);

    executor = Executors.newFixedThreadPool(1);
    printer = setupPrinter(statisticsPath);
  }

  private CSVPrinter setupPrinter(String path) throws IOException {
    val f = new File(path);
    val p = f.getParentFile();
    if (!f.canWrite() && f.exists() && p.isDirectory() && !p.canWrite()) {
      throw new RuntimeException("cannot write to path " + f.getAbsolutePath());
    }
    return CSVFormat.DEFAULT.withHeader("msg-id", "sent", "recv").print(f, Charsets.UTF_8);
  }

  public void exportAsync(long msgIdFrom, long msgIdTo, SortedMap<Long, MessageStats> stats) {
    log.info("exporting msgs from {} to {} async; file: {}", msgIdFrom, msgIdTo, statisticsPath);
    executor.execute(() -> exportRange(msgIdFrom, msgIdTo, stats));
  }

  public void exportRemaining(long msgIdFrom, SortedMap<Long, MessageStats> stats) {
    log.info("exporting all msgs starting with {}; file {}", msgIdFrom, statisticsPath);
    exportRange(msgIdFrom, stats);
  }

  private synchronized void exportRange(long msgIdFrom, SortedMap<Long, MessageStats> stats) {
    val lastKey = stats.keySet().toSortedSet().last();
    exportRange(msgIdFrom, lastKey, stats);
  }

  private synchronized void exportRange(long msgIdFrom, long msgIdTo,
      SortedMap<Long, MessageStats> msgStats) {
    log.info("exporting from {} to {}; file {}", msgIdFrom, msgIdTo, statisticsPath);

    val partitioned = msgStats.partition(t -> t._1 >= msgIdFrom && t._1 <= msgIdTo);
    log.debug("partitioned: {}", partitioned);

    val inRange = partitioned._1;
    inRange.forEach((msgId, stats) -> {
      try {
        printer
            .printRecord(msgId, fromInstantOpt(stats.getSent()), fromInstantOpt(stats.getRecv()));
      } catch (IOException e) {
        log.error("cannot print {}: {}", msgId, e.getMessage());
      }
    });
    try {
      printer.flush();
    } catch (IOException e) {
      log.error("cannot flush csv export: {}", e.getMessage());
    }
    log.info("exporting from {} finished", msgIdFrom);
  }

  private String fromInstantOpt(Option<Instant> inst) {
    return inst
        .map(instant -> formatter
            .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault())))
        .getOrElse("");
  }
}
