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

import at.sbaresearch.mqtt4android.analysis.statistics.Statistics.MessageStats.MessageStatsBuilder;
import io.vavr.collection.*;
import io.vavr.control.Option;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

import static io.vavr.API.*;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class Statistics {

  @NonFinal
  SortedMap<Long, MessageStats> msgStats = TreeMap.empty();
  @NonFinal
  long lastMsgIdRecv = -1;
  @NonFinal
  long lastMsgIdSent = -1;
  @NonFinal
  long lastMsgIdRecvExported = -1;
  Clock clk = Clock.systemDefaultZone();
  StatisticsExporter statisticsExporter;

  public synchronized void messageSent(long msgId) {
    MessageStatsBuilder msg = defaultStats();
    msgStats = msgStats.put(msgId,
        msg.sent(Some(clk.instant())).build());
    lastMsgIdSent = msgId;
  }

  public synchronized void messagesReceivedUpToIncluding(long idRecv) {
    log.debug("stats: msg recv up to including {}", idRecv);
    if (idRecv > lastMsgIdRecv) {
      log.info("stats: msg recv up to including {}, last recv {}", idRecv, lastMsgIdRecv);
      if (idRecv > lastMsgIdSent) {
        log.warn(
            "stats: received higher message delivered notification than sent, ignoring msgId {}",
            idRecv);
        return;
      } else {
        List.range(lastMsgIdRecv + 1, idRecv + 1)
            .forEach(this::messageReceived);
        lastMsgIdRecv = idRecv;
        notifyBatchExport(lastMsgIdRecvExported, lastMsgIdRecv, msgStats);
      }
    }
  }

  public synchronized void messageReceived(long msgId) {
    log.debug("stats: msg recv {}", msgId);
    val msg = msgStats.getOrElse(msgId, defaultStats().build()).toBuilder();
    msgStats = msgStats.put(msgId,
        msg.recv(Some(clk.instant())).build());
  }

  private void notifyBatchExport(long lastExported, long lastRecv,
      SortedMap<Long, MessageStats> currentStats) {
    // export this range if range is big enough
    if (lastRecv > lastExported + 100) {
      statisticsExporter.exportAsync(lastExported + 1, lastRecv, currentStats);
      lastMsgIdRecvExported = lastRecv;
    }
  }

  public void exportUnconditionally() {
    statisticsExporter.exportRemaining(lastMsgIdRecvExported + 1, msgStats);
  }

  private MessageStats.MessageStatsBuilder defaultStats() {
    return MessageStats.builder()
        .sent(None())
        .recv(None());
  }

  @Value
  @Builder(toBuilder = true)
  public static class MessageStats {
    Option<Instant> sent;
    Option<Instant> recv;
  }
}
