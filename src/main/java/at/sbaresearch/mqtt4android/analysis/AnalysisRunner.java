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

package at.sbaresearch.mqtt4android.analysis;

import at.sbaresearch.mqtt4android.analysis.actions.ConverstationActions;
import at.sbaresearch.mqtt4android.analysis.actions.LoginAction;
import at.sbaresearch.mqtt4android.analysis.schedule.RandomMessageSender;
import at.sbaresearch.mqtt4android.analysis.statistics.Statistics;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;
import java.util.Random;

import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalysisRunner {

  LoginAction loginAction;
  ConverstationActions converstationActions;
  RandomMessageSender randomMessageSender;
  LogoutWatchdog logoutWatchdog;
  DeliveredWatchdog deliveredWatchdog;
  Statistics statistics;
  long seed;
  int timelimitMins;

  public void run() throws Exception {
    setup();

    val random = new Random(seed);
    startMessageSending(random);
  }

  private void setup() {
    log.info("setup");
    setupConversationList();
    deleteConverstationHistory();
    setupLogoutDetection();
    setupDeliveredHandling();
    setupExportOnShutdown();
  }

  private void setupExportOnShutdown() {
    Runtime.getRuntime().addShutdownHook(new Thread("export-statistics") {
      public void run() {
        statistics.exportUnconditionally();
      }
    });
  }

  private void setupConversationList() {
    log.info("login and load conversation");
    loginAction.loadPage();
    loginAction.login();
    converstationActions.loadConversation();
    log.info("login and load conversation - done");
  }

  private void deleteConverstationHistory() {
    converstationActions.deleteAndReloadConversation();
  }

  private void setupLogoutDetection() {
    logoutWatchdog.onLogout((v) -> setupConversationList());
  }

  private void startMessageSending(Random random) {
    if (timelimitMins > 0) {
      val until = Instant.now().plus(timelimitMins, MINUTES);
      randomMessageSender.start(random, until);
    } else {
      randomMessageSender.start(random);
    }
  }

  private void setupDeliveredHandling() {
    deliveredWatchdog.onDelivered(statistics::messagesReceivedUpToIncluding);
  }

}
