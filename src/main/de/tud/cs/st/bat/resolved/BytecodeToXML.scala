/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package resolved

import java.io.{ File, FileInputStream }
import java.io.IOException

import scala.xml._

import de.tud.cs.st.util.UTF8Println

import reader.Java6Framework

/**
 * Creates an XML representation of each class file and pretty prints the
 * XML representation to the console.
 *
 * @author Michael Eichberg
 */
object BytecodeToXML extends UTF8Println {

    def main(args: Array[String]): Unit = {

        println("""
                |<!--
				|	BytecodeToXML (c) 2009, 2011
				|	Software Technology Group
				|	Department of Computer Science,
				|	Technische Universität Darmstadt
				|	Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
				|-->
                """.stripMargin)

        val pp = new PrettyPrinter(160, 4)
        for (arg ← args) {
            try {
                println(
                    pp.format(
                        (
                            if ((new File(arg)).exists)
                                Java6Framework.ClassFile(() ⇒ new FileInputStream(arg))
                            else
                                Java6Framework.ClassFile(() ⇒ Class.forName(arg).getResourceAsStream(arg.substring(arg.lastIndexOf('.') + 1) + ".class"))
                        ).toXML
                    )
                )
            } catch {
                case io: IOException ⇒ println("Error while reading the file: " + arg); io.getMessage();
                case t: Throwable    ⇒ println("Unknown error while loading: " + arg + "\n"); t.getMessage;
            }
        }

    }
}
