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
package de.tud.cs.st.bat.resolved
package dependency

import DependencyType._

/**
 * Interface used by <code>DependencyExtractor</code> to get unique numerical identifiers
 * and to export extracted dependency. Classes that have this trait mixed in can be used
 * as <code>DependencyExtractor</code>'s class parameter and for example collect the
 * extracted dependencies.
 *
 * IMPLEMENTATION NOTE: In general, every the two related <code>getID</code> methods
 * should return the same result for parameters that refer to the same source element.
 *
 * @author Thomas Schlosser
 */
trait DependencyBuilder {

    /**
     * Gets a unique numerical identifier for the given class file.
     *
     * @param classFile The class file, a unique identifier should be returned for.
     * @return A unique numerical value that identifies the given class file.
     */
    def getID(classFile: ClassFile): Int
    /**
     * Gets a unique numerical identifier for the given type.
     *
     * @param t The type, a unique identifier should be returned for.
     * @return A unique numerical value that identifies the type.
     */
    def getID(t: Type): Int

    /**
     * Gets a unique numerical identifier for the given pair of type and field.
     *
     * @param definingObjectType The type of the class that defines the given field.
     * @param field The field, a unique identifier should be returned for.
     * @return A unique numerical value that identifies the given pair of type and field.
     */
    def getID(definingObjectType: ObjectType, field: Field): Int
    /**
     * Gets a unique numerical identifier for the given pair of type and field name.
     *
     * @param definingObjectType The type of the class that defines the field
     *                           that is related to the given field name.
     * @param fieldName The name of the field, a unique identifier should be returned for.
     * @return A unique numerical value that identifies the given pair of type and field name.
     */
    def getID(definingObjectType: ObjectType, fieldName: String): Int

    /**
     * Gets a unique numerical identifier for the given pair of type and method.
     *
     * @param definingObjectType The type of the class that defines the given method.
     * @param method The method, a unique identifier should be returned for.
     * @return A unique numerical value that identifies the given pair of type and method.
     */
    def getID(definingObjectType: ObjectType, method: Method): Int
    /**
     * Gets a unique numerical identifier for the given triple of type, method name and method descriptor.
     *
     * @param definingObjectType The type of the class that defines the method that
     *                           is related to the given method name and descriptor.
     * @param methodName The name of the method, a unique identifier should be returned for.
     * @param methodDescriptor The method descriptor of the method, a unique identifier should be returned for.
     * @return A unique numerical value that identifies the given triple of type, method name and method descriptor.
     */
    def getID(definingObjectType: ObjectType, methodName: String, methodDescriptor: MethodDescriptor): Int

    /**
     * Adds a dependency of the given type between the source and target.
     *
     * @param source The ID of the origin source element.
     * @param target The ID of the target source element.
     * @param dType The type of the dependency.
     */
    def addDependency(source: Int, target: Int, dType: DependencyType)

}