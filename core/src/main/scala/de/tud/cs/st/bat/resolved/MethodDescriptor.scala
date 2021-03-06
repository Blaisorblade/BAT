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

/**
 * A method descriptor represents the parameters that the method takes and
 * the value that it returns.
 *
 * @author Michael Eichberg
 */
case class MethodDescriptor(parameterTypes: Seq[FieldType], returnType: Type) {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toUMLNotation: String = {
        "("+{
            if (parameterTypes.size == 0)
                ""
            else
                (parameterTypes.head.toJava /: parameterTypes.tail)(_+", "+_.toJava)
        }+") : "+returnType.toJava
    }

}
object MethodDescriptor {

    def apply(md: String): MethodDescriptor = {

        var index = 1 // we are not interested in the leading '('

        var parameterTypes: List[FieldType] = Nil
        while (md.charAt(index) != ')') {
            val (ft, nextIndex) = parseParameterType(md, index)
            parameterTypes = ft :: parameterTypes
            index = nextIndex
        }
        parameterTypes = parameterTypes.reverse

        MethodDescriptor(
            parameterTypes,
            ReturnType(md.substring(index + 1))
        )
    }

    private def parseParameterType(md: String, startIndex: Int): (FieldType, Int) = {
        val td = md.charAt(startIndex)
        (td: @scala.annotation.switch) match {
            case 'L' ⇒
                val endIndex = md.indexOf(';', startIndex + 1)
                ( // this is the return tuple
                    ObjectType(md.substring(startIndex + 1, endIndex)),
                    endIndex + 1
                )
            case '[' ⇒
                val (ft, index) = parseParameterType(md, startIndex + 1)
                ( // this is the return tuple
                    ArrayType(ft),
                    index
                )
            case _ ⇒
                ( // this is the return tuple
                    FieldType(td.toString),
                    startIndex + 1
                )
        }
    }
}







