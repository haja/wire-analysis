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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@AllArgsConstructor
public class AnalysisApplication implements CommandLineRunner {

  private final AnalysisRunner runner;

  public static void main(String[] args) {
    SpringApplication.run(AnalysisApplication.class, args);
  }

  @Override
  public void run(String... args) {
    try {
      runner.run();
    } catch (Exception e) {
      log.error("exception: {}", e.getMessage(), e);
      System.exit(1);
    }
  }
}
