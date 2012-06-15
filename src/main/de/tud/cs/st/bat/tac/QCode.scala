/*
 * License (BSD Style License):
 * Copyright (c) 2012
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

/**
 * A value on the stack or a value stored in one of the local variables.
 * @author Michael Eichberg
 */
sealed trait Value {
    /**
     * The computational type of the value.
     */
    def computationalType: ComputationalType
}

/*
 *
 */
case object UnusableValue {
    def computationalType: ComputationalType = throw new Error("")
}

case class TypedValue(valueType: Type) extends Value {
    def computationalType = valueType.computationalType
}

object TypedValue {
    val BooleanValue = TypedValue(BooleanType)
    val ByteValue = TypedValue(ByteType)
    val CharValue = TypedValue(CharType)
    val ShortValue = TypedValue(ShortType)
    val IntegerValue = TypedValue(IntegerType)
    val LongValue = TypedValue(LongType)
    val FloatValue = TypedValue(FloatType)
    val DoubleValue = TypedValue(DoubleType)
}

case class ComputationalTypeValue(val computationalType: ComputationalType) extends Value

case object NullValue extends Value {
    def computationalType = ComputationalTypeReference
}

sealed protected trait SomeMemoryLayout {
    def locals: IndexedSeq[Value]
    def operands: List[Value]
    def update(instruction: BytecodeInstruction): SomeMemoryLayout
}
/*
 * After the execution of a (a,i,f,l,d)return instruction, the stack/heap is no longer allowed to be used.s
 */
object NoMemoryLayout extends SomeMemoryLayout {
    def locals: IndexedSeq[Value] = {
        throw new Error("no memory layout available")
    }
    def operands: List[Value] = {
        throw new Error("no memory layout available")
    }
    def update(instruction: BytecodeInstruction): SomeMemoryLayout = {
        throw new Error("no memory layout available")
    }
}
class MemoryLayout(val operands: List[Value], val locals: IndexedSeq[Value]) extends SomeMemoryLayout {

    def update(instruction: BytecodeInstruction): SomeMemoryLayout = {
        import annotation.switch

        (instruction.opcode: @switch) match {
            case 50 /*aaload*/ ⇒ new MemoryLayout(
                {
                    val arrayref = operands.tail.head
                    arrayref match {
                        case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType) :: operands.tail.tail
                        /* TODO Do we want to handle: case NullValue => …*/
                        case _                                    ⇒ ComputationalTypeValue(ComputationalTypeReference) :: (operands.tail.tail)
                    }
                },
                locals)
            case 83 /*aastore*/      ⇒ new MemoryLayout(operands.tail.tail.tail, locals)
            case 1 /*aconst_null*/   ⇒ new MemoryLayout(NullValue :: operands, locals)
            case 25 /*aload*/        ⇒ new MemoryLayout(locals(instruction.asInstanceOf[ALOAD].lvIndex) :: operands, locals)
            case 42 /*aload_0*/      ⇒ new MemoryLayout(locals(0) :: operands, locals)
            case 43 /*aload_1*/      ⇒ new MemoryLayout(locals(1) :: operands, locals)
            case 44 /*aload_2*/      ⇒ new MemoryLayout(locals(2) :: operands, locals)
            case 45 /*aload_3*/      ⇒ new MemoryLayout(locals(3) :: operands, locals)
            case 189 /*anewarray*/   ⇒ new MemoryLayout(TypedValue(ArrayType(instruction.asInstanceOf[ANEWARRAY].componentType)) :: (operands.tail), locals)
            case 176 /*areturn*/     ⇒ NoMemoryLayout
            case 190 /*arraylength*/ ⇒ new MemoryLayout(TypedValue(IntegerType) :: (operands.tail), locals)
            case 58 /*astore*/       ⇒ new MemoryLayout(operands.tail, locals.updated(instruction.asInstanceOf[ASTORE].lvIndex, locals.head))
            case 75 /*astore_0*/     ⇒ new MemoryLayout(operands.tail, locals.updated(0, locals.head))
            case 76 /*astore_1*/     ⇒ new MemoryLayout(operands.tail, locals.updated(1, locals.head))
            case 77 /*astore_2*/     ⇒ new MemoryLayout(operands.tail, locals.updated(2, locals.head))
            case 78 /*astore_3*/     ⇒ new MemoryLayout(operands.tail, locals.updated(3, locals.head))
            case 191 /*athrow*/ ⇒ new MemoryLayout(
                {
                    val v = operands.head
                    v match {
                        case NullValue ⇒ List(TypedValue(InstructionExceptions.NullPointerException))
                        case _         ⇒ List(v)
                    }
                },
                locals)
            case 51 /*baload*/           ⇒ null
            case 84 /*bastore*/          ⇒ null
            case 16 /*bipush*/           ⇒ null
            case 52 /*caload*/           ⇒ null
            case 85 /*castore*/          ⇒ null
            case 192 /*checkcast*/       ⇒ null
            case 144 /*d2f*/             ⇒ null
            case 142 /*d2i*/             ⇒ null
            case 143 /*d2l*/             ⇒ null
            case 99 /*dadd*/             ⇒ null
            case 49 /*daload*/           ⇒ null
            case 82 /*dastore*/          ⇒ null
            case 152 /*dcmpg*/           ⇒ null
            case 151 /*dcmpl*/           ⇒ null
            case 14 /*dconst_0*/         ⇒ null
            case 15 /*dconst_1*/         ⇒ null
            case 111 /*ddiv*/            ⇒ null
            case 24 /*dload*/            ⇒ null
            case 38 /*dload_0*/          ⇒ new MemoryLayout(operands.tail, locals.updated(0, locals.head))
            case 39 /*dload_1*/          ⇒ new MemoryLayout(operands.tail, locals.updated(1, locals.head))
            case 40 /*dload_2*/          ⇒ new MemoryLayout(operands.tail, locals.updated(2, locals.head))
            case 41 /*dload_3*/          ⇒ new MemoryLayout(operands.tail, locals.updated(3, locals.head))
            case 107 /*dmul*/            ⇒ null
            case 119 /*dneg*/            ⇒ null
            case 115 /*drem*/            ⇒ null
            case 175 /*dreturn*/         ⇒ NoMemoryLayout
            case 57 /*dstore*/           ⇒ null
            case 71 /*dstore_0*/         ⇒ null
            case 72 /*dstore_1*/         ⇒ null
            case 73 /*dstore_2*/         ⇒ null
            case 74 /*dstore_3*/         ⇒ null
            case 103 /*dsub*/            ⇒ null
            case 89 /*dup*/              ⇒ null
            case 90 /*dup_x1*/           ⇒ null
            case 91 /*dup_x2*/           ⇒ null
            case 92 /*dup2*/             ⇒ null
            case 93 /*dup2_x1*/          ⇒ null
            case 94 /*dup2_x2*/          ⇒ null
            case 141 /*f2d*/             ⇒ null
            case 139 /*f2i*/             ⇒ null
            case 140 /*f2l*/             ⇒ null
            case 98 /*fadd*/             ⇒ null
            case 48 /*faload*/           ⇒ null
            case 81 /*fastore*/          ⇒ null
            case 150 /*fcmpg*/           ⇒ null
            case 149 /*fcmpl*/           ⇒ null
            case 11 /*fconst_0*/         ⇒ null
            case 12 /*fconst_1*/         ⇒ null
            case 13 /*fconst_2*/         ⇒ null
            case 110 /*fdiv*/            ⇒ null
            case 23 /*fload*/            ⇒ null
            case 34 /*fload_0*/          ⇒ null
            case 35 /*fload_1*/          ⇒ null
            case 36 /*fload_2*/          ⇒ null
            case 37 /*fload_3*/          ⇒ null
            case 106 /*fmul*/            ⇒ null
            case 118 /*fneg*/            ⇒ null
            case 114 /*frem*/            ⇒ null
            case 174 /*freturn*/         ⇒ NoMemoryLayout
            case 56 /*fstore*/           ⇒ null
            case 67 /*fstore_0*/         ⇒ null
            case 68 /*fstore_1*/         ⇒ null
            case 69 /*fstore_2*/         ⇒ null
            case 70 /*fstore_3*/         ⇒ null
            case 102 /*fsub*/            ⇒ null
            case 180 /*getfield*/        ⇒ null
            case 178 /*getstatic*/       ⇒ null
            case 167 /*goto*/            ⇒ null
            case 200 /*goto_w*/          ⇒ null
            case 145 /*i2b*/             ⇒ null
            case 146 /*i2c*/             ⇒ null
            case 135 /*i2d*/             ⇒ null
            case 134 /*i2f*/             ⇒ null
            case 133 /*i2l*/             ⇒ null
            case 147 /*i2s*/             ⇒ null
            case 96 /*iadd*/             ⇒ null
            case 46 /*iaload*/           ⇒ null
            case 126 /*iand*/            ⇒ null
            case 79 /*iastore*/          ⇒ null
            case 2 /*iconst_m1*/         ⇒ null
            case 3 /*iconst_0*/          ⇒ null
            case 4 /*iconst_1*/          ⇒ null
            case 5 /*iconst_2*/          ⇒ null
            case 6 /*iconst_3*/          ⇒ null
            case 7 /*iconst_4*/          ⇒ null
            case 8 /*iconst_5*/          ⇒ null
            case 108 /*idiv*/            ⇒ null
            case 165 /*if_acmpeq*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 166 /*if_acmpne*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 159 /*if_icmpeq*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 160 /*if_icmpne*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 161 /*if_icmplt*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 162 /*if_icmpge*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 163 /*if_icmpgt*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 164 /*if_icmple*/       ⇒ new MemoryLayout(operands.tail.tail, locals)
            case 153 /*ifeq*/            ⇒ new MemoryLayout(operands.tail, locals)
            case 154 /*ifne*/            ⇒ new MemoryLayout(operands.tail, locals)
            case 155 /*iflt*/            ⇒ new MemoryLayout(operands.tail, locals)
            case 156 /*ifge*/            ⇒ new MemoryLayout(operands.tail, locals)
            case 157 /*ifgt*/            ⇒ new MemoryLayout(operands.tail, locals)
            case 158 /*ifle*/            ⇒ new MemoryLayout(operands.tail, locals)
            case 199 /*ifnonnull*/       ⇒ new MemoryLayout(operands.tail, locals)
            case 198 /*ifnull*/          ⇒ new MemoryLayout(operands.tail, locals)
            case 132 /*iinc*/            ⇒ this
            case 21 /*iload*/            ⇒ new MemoryLayout(locals(instruction.asInstanceOf[ILOAD].lvIndex) :: operands, locals)
            case 26 /*iload_0*/          ⇒ null
            case 27 /*iload_1*/          ⇒ null
            case 28 /*iload_2*/          ⇒ null
            case 29 /*iload_3*/          ⇒ null
            case 104 /*imul*/            ⇒ null
            case 116 /*ineg*/            ⇒ null
            case 193 /*instanceof*/      ⇒ null
            case 186 /*invokedynamic*/   ⇒ null
            case 185 /*invokeinterface*/ ⇒ null
            case 183 /*invokespecial*/   ⇒ null
            case 184 /*invokestatic*/    ⇒ null
            case 182 /*invokevirtual*/   ⇒ null
            case 128 /*ior*/             ⇒ null
            case 112 /*irem*/            ⇒ null
            case 172 /*ireturn*/         ⇒ NoMemoryLayout
            case 120 /*ishl*/            ⇒ null
            case 122 /*ishr*/            ⇒ null
            case 54 /*istore*/           ⇒ null
            case 59 /*istore_0*/         ⇒ null
            case 60 /*istore_1*/         ⇒ null
            case 61 /*istore_2*/         ⇒ null
            case 62 /*istore_3*/         ⇒ null
            case 100 /*isub*/            ⇒ null
            case 124 /*iushr*/           ⇒ null
            case 130 /*ixor*/            ⇒ null
            case 168 /*jsr*/             ⇒ null
            case 201 /*jsr_w*/           ⇒ null
            case 138 /*l2d*/             ⇒ null
            case 137 /*l2f*/             ⇒ null
            case 136 /*l2i*/             ⇒ null
            case 97 /*ladd*/             ⇒ null
            case 47 /*laload*/           ⇒ null
            case 127 /*land*/            ⇒ null
            case 80 /*lastore*/          ⇒ null
            case 148 /*lcmp*/            ⇒ null
            case 9 /*lconst_0*/          ⇒ null
            case 10 /*lconst_1*/         ⇒ null
            case 18 /*ldc*/              ⇒ null
            case 19 /*ldc_w*/            ⇒ null
            case 20 /*ldc2_w*/           ⇒ null
            case 109 /*ldiv*/            ⇒ null
            case 22 /*lload*/            ⇒ null
            case 30 /*lload_0*/          ⇒ null
            case 31 /*lload_1*/          ⇒ null
            case 32 /*lload_2*/          ⇒ null
            case 33 /*lload_3*/          ⇒ null
            case 105 /*lmul*/            ⇒ null
            case 117 /*lneg*/            ⇒ null
            case 171 /*lookupswitch*/    ⇒ null
            case 129 /*lor*/             ⇒ null
            case 113 /*lrem*/            ⇒ null
            case 173 /*lreturn*/         ⇒ NoMemoryLayout
            case 121 /*lshl*/            ⇒ null
            case 123 /*lshr*/            ⇒ null
            case 55 /*lstore*/           ⇒ null
            case 63 /*lstore_0*/         ⇒ null
            case 64 /*lstore_1*/         ⇒ null
            case 65 /*lstore_2*/         ⇒ null
            case 66 /*lstore_3*/         ⇒ null
            case 101 /*lsub*/            ⇒ null
            case 125 /*lushr*/           ⇒ null
            case 131 /*lxor*/            ⇒ null
            case 194 /*monitorenter*/    ⇒ null
            case 195 /*monitorexit*/     ⇒ null
            case 197 /*multianewarray*/  ⇒ null
            case 187 /*new*/             ⇒ null
            case 188 /*newarray*/        ⇒ null
            case 0 /*nop*/               ⇒ null
            case 87 /*pop*/              ⇒ null
            case 88 /*pop2*/             ⇒ null
            case 181 /*putfield*/        ⇒ null
            case 179 /*putstatic*/       ⇒ null
            case 169 /*ret*/             ⇒ null
            case 177 /*return*/          ⇒ null
            case 53 /*saload*/           ⇒ null
            case 86 /*sastore*/          ⇒ null
            case 17 /*sipush*/           ⇒ null
            case 95 /*swap*/             ⇒ null
            case 170 /*tableswitch*/     ⇒ null
            case 196 /*wide*/            ⇒ this // the instructions which are modified by a wide instruction already take care of the effect of wide
        }
    }
}

case class QCode {

}
object QCode {
    /**
     * @param classFile Some class file.
     * @param method A method with a body of the respective given class file.
     */
    def apply(classFile: ClassFile, method: Method): QCode = {
        val code = method.body.get
        val initialLocals = {
            var locals: Vector[Value] = Vector.empty
            var localVariableIndex = 0

            if (!method.isStatic) {
                val thisType = classFile.thisClass
                locals = locals.updated(localVariableIndex, TypedValue(thisType))
                localVariableIndex += thisType.computationalType.operandSize
            }
            for (parameterType ← method.descriptor.parameterTypes) {
                val ct = parameterType.computationalType
                locals = locals.updated(localVariableIndex, TypedValue(parameterType))
                localVariableIndex += ct.operandSize
            }
            locals
        }

        // true if the instruction with the respective program counter is already transformed
        val transformed = new Array[Boolean](code.instructions.length)

        var worklist: List[(Int /*program counter*/ , MemoryLayout /* the layout of the locals and stack before the instruction with the respective pc is executed */ )] = List((0, new MemoryLayout(Nil, initialLocals)))
        // the instructions which are at the beginning of a catch block are also added to the catch block
        for (exceptionHandler ← code.exceptionHandlers) {

        }

        while (worklist.nonEmpty) {
            val (pc, memoryLayout) = worklist.head
            worklist = worklist.tail
            if (!transformed(pc)) {

                memoryLayout.update(code.instructions(pc))

                // prepare for the transformation of the next instruction
                transformed(pc) = true
            }
        }

        null
    }
}

trait LValue extends RValue
trait RValue

case class Parameter(val id: Int) extends RValue

case class This extends RValue

trait Statement
trait Expression extends RValue
trait UnaryExpression extends Expression {
    def exp : LValue
}
abstract class BinaryExpression extends Expression {
    def lExp: LValue
    def rExp: LValue
}
case class AndExpression(val lExp: LValue, val rExp: LValue)  extends BinaryExpression

case object MonitorEnter extends Statement
case object MonitorExit extends Statement
case class Assignment(lValue: LValue, rValue: RValue) extends Statement

object Demo extends scala.App {
//    new SEStatement(MonitorEnter, new RValue[ReferenceType] {}) {}
}