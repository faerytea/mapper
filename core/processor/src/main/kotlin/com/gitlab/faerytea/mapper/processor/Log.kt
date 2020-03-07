/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Processor.
 *
 * Mapper Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapper Processor.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.processor

import java.io.PrintStream

object Log {
    var enabled: Boolean = false
    var destination: PrintStream = System.out

    inline fun note(msg: () -> Any?) {
        if (enabled) {
            destination.println(msg())
        }
    }
}