package de.tud.cs.st.bat.resolved
package dependency
import org.scalatest.FunSuite
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import org.scalatest.Reporter
import org.scalatest.Stopper
import org.scalatest.Tracker
import org.scalatest.events.TestStarting
import de.tud.cs.st.bat.resolved.reader.Java6Framework
import org.scalatest.events.TestSucceeded
import org.scalatest.events.TestFailed
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DependencyType._

/**
 * @author Thomas Schlosser
 */
@RunWith(classOf[JUnitRunner])
class DepExtractorTest extends FunSuite with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

  type Dependency = (String, String, DependencyType)
  type Dependencies = List[Dependency]

  /*
   * All class files stored in the zip file "DepExtractorTestProject" found in the test data directory.
   */
  private val testClasses = {
    var classFiles = List.empty[de.tud.cs.st.bat.resolved.ClassFile]
    // The location of the "test/data" directory depends on the current directory used for 
    // running this test suite... i.e. whether the current directory is the directory where
    // this class / this source file is stored or the BAT's root directory. 
    var file = new File("../../../../../../../test/classfiles/DepExtractorTestProject.zip")
    if (!file.exists()) file = new File("test/classfiles/DepExtractorTestProject.zip")
    if (!file.exists() || !file.isFile || !file.canRead || !file.getName.endsWith(".zip")) throw new Exception("Required zip file 'DepExtractorTestProject.zip' in 'test/classfiles' could not be found or read!")
    val zipfile = new ZipFile(file)
    val zipentries = (zipfile).entries
    while (zipentries.hasMoreElements) {
      val zipentry = zipentries.nextElement
      if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
        classFiles :+= Java6Framework.ClassFile(() => zipfile.getInputStream(zipentry))
      }
    }
    classFiles
  }

  test("Dependency extraction") {
    // create dependency builder that collects all added dependencies
    val depBuilder = new DepBuilder {
      var nodes = Array.empty[String]
      var deps: Dependencies = List()

      def getID(identifier: String): Int = {
        var index = nodes.indexOf(identifier)
        if (index == -1) {
          nodes :+= identifier
          index = nodes.length - 1
        }
        index
      }

      def addDep(src: Int, trgt: Int, dType: DependencyType) = {
        val srcNode = nodes(src)
        val trgtNode = nodes(trgt)
        //        println("addDep: " + srcNode + "--[" + dType + "]-->" + trgtNode)
        deps :+= (srcNode, trgtNode, dType)
      }
    }
    val dependencyExtractor = new DepExtractor(depBuilder)

    for (classFile <- testClasses) {
      // process classFile using dependency extractor
      dependencyExtractor.process(classFile)
    }

    //verification...
    implicit val aDeps = new AssertableDependencies(depBuilder.deps)

    assertTestClass
    assertTestInterface
    assertMarkerInterface
    assertDeprecatedInterface
    assertFieldsClass
    assertOuterAndInnerClass
    assertEnclosingMethodAndInnerClass
    assertExceptionTestClass
    assertTestAnnotation
    assertAnnotationDefaultAttributeTestClass
    assertInstructionsTestClass
    //TODO: generics have also to be considered

    assert(aDeps.deps.isEmpty, "Too many [" + aDeps.deps.size + "] dependencies have been extracted:\n" + aDeps.deps.mkString("\n"))
  }

  private def assertTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    import java.util.ArrayList;
    //    import java.util.List;
    //    
    //    public class TestClass implements TestInterface {
    aDeps.assertDependency("test.TestClass", "test.TestInterface", IMPLEMENTS)
    aDeps.assertDependency("test.TestClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.TestClass")
    //        public void testMethod() {
    aDeps.assertDependency("test.TestClass.testMethod()", "test.TestClass", IS_DEFINED_IN)
    //    	List<String> list = new ArrayList<String>();
    //TODO: add: aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List", USED_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.ArrayList", CREATES)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.ArrayList", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.ArrayList.<init>()", CALLS_METHOD)
    //    	list.add(null);
    //TODO: check why this fails: aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List.add(java.lang.String)", CALLS_METHOD)
    // -> Parameter type results from a generic type. Hence, it refers to java.lang.Object.
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List.add(java.lang.Object)", CALLS_INTERFACE_METHOD)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.lang.Object", USES_PARAMETER_TYPE)
    //        }
    //    
    //        public String testMethod(Integer i, int j) {
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "test.TestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURNS)
    //    	if (i != null && i.intValue() > j) {
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.intValue()", CALLS_METHOD)
    //    	    return i.toString();
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.toString()", CALLS_METHOD)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_RETURN_TYPE)
    //    	}
    //    	return String.valueOf(j);
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String.valueOf(int)", CALLS_METHOD)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_RETURN_TYPE)
    //        }
    //    }
  }

  private def assertTestInterface(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    public interface TestInterface {
    aDeps.assertDependency("test.TestInterface", "java.lang.Object", EXTENDS)
    //        void testMethod();
    aDeps.assertDependency("test.TestInterface.testMethod()", "test.TestInterface", IS_DEFINED_IN)
    //    
    //        String testMethod(Integer i, int j);
    aDeps.assertDependency("test.TestInterface.testMethod(java.lang.Integer, int)", "test.TestInterface", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.String", RETURNS)
    //    }
  }

  private def assertMarkerInterface(implicit aDeps: AssertableDependencies) {
    //    package test.sub;
    //
    //    public interface MarkerInterface {
    aDeps.assertDependency("test.sub.MarkerInterface", "java.lang.Object", EXTENDS)
    //    
    //    }
  }

  private def assertDeprecatedInterface(implicit aDeps: AssertableDependencies) {
    //    package test.sub;
    //
    //    import test.TestInterface;
    //    
    //    @Deprecated
    //    public interface DeprecatedInterface extends TestInterface, MarkerInterface {
    aDeps.assertDependency("test.sub.DeprecatedInterface", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.sub.DeprecatedInterface", "test.TestInterface", IMPLEMENTS)
    aDeps.assertDependency("test.sub.DeprecatedInterface", "test.sub.MarkerInterface", IMPLEMENTS)
    aDeps.assertDependency("test.sub.DeprecatedInterface", "java.lang.Deprecated", ANNOTATED_WITH)
    //    
    //        @Deprecated
    //        public void deprecatedMethod();
    aDeps.assertDependency("test.sub.DeprecatedInterface.deprecatedMethod()", "test.sub.DeprecatedInterface", IS_DEFINED_IN)
    aDeps.assertDependency("test.sub.DeprecatedInterface.deprecatedMethod()", "java.lang.Deprecated", ANNOTATED_WITH)
    //    
    //        public void methodDeprParam(@Deprecated int i);
    aDeps.assertDependency("test.sub.DeprecatedInterface.methodDeprParam(int)", "test.sub.DeprecatedInterface", IS_DEFINED_IN)
    aDeps.assertDependency("test.sub.DeprecatedInterface.methodDeprParam(int)", "java.lang.Deprecated", PARAMETER_ANNOTATED_WITH)
    //    }
  }

  private def assertFieldsClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    public class FieldsClass {
    aDeps.assertDependency("test.FieldsClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.FieldsClass")
    //        public final static String CONSTANT = "constant";
    aDeps.assertDependency("test.FieldsClass.CONSTANT", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.CONSTANT", "java.lang.String", IS_OF_TYPE)
    aDeps.assertDependency("test.FieldsClass.CONSTANT", "java.lang.String", USES_CONSTANT_VALUE_OF_TYPE)
    //        private Integer i;
    aDeps.assertDependency("test.FieldsClass.i", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.i", "java.lang.Integer", IS_OF_TYPE)
    //    
    //        @Deprecated
    //        protected int deprecatedField;
    aDeps.assertDependency("test.FieldsClass.deprecatedField", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.deprecatedField", "java.lang.Deprecated", ANNOTATED_WITH)
    //    
    //        private Integer readField() {
    aDeps.assertDependency("test.FieldsClass.readField()", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.readField()", "java.lang.Integer", RETURNS)
    //    	return i;
    aDeps.assertDependency("test.FieldsClass.readField()", "test.FieldsClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.FieldsClass.readField()", "test.FieldsClass.i", READS_FIELD)
    aDeps.assertDependency("test.FieldsClass.readField()", "java.lang.Integer", USES_FIELD_READ_TYPE)
    //        }
    //    
    //        private void writeField(Integer j) {
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
    //    	i = j;
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "test.FieldsClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "test.FieldsClass.i", WRITES_FIELD)
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", USES_FIELD_WRITE_TYPE)
    //        }
    //    
    //        public Integer readWrite(Integer j) {
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", RETURNS)
    //    	Integer result = readField();
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass.readField()", CALLS_METHOD)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USES_RETURN_TYPE)
    //    	writeField(j);
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass.writeField(java.lang.Integer)", CALLS_METHOD)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USES_PARAMETER_TYPE)
    //    	return result;
    //        }
    //    }
  }

  private def assertOuterAndInnerClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    public class OuterClass {
    aDeps.assertDependency("test.OuterClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.OuterClass")
    //        class InnerClass {
    aDeps.assertDependency("test.OuterClass$InnerClass", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.OuterClass$InnerClass", "test.OuterClass", IS_DEFINED_IN)
    //	//implicit field:
    aDeps.assertDependency("test.OuterClass$InnerClass.this$0", "test.OuterClass$InnerClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.OuterClass$InnerClass.this$0", "test.OuterClass", IS_OF_TYPE)
    //		public InnerClass(Integer i) {
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass$InnerClass", IS_DEFINED_IN)
    //	//implicit constructor parameter:
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "java.lang.Object.<init>()", CALLS_METHOD)
    //	// write to implicit field:
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass$InnerClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass$InnerClass.this$0", WRITES_FIELD)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass", USES_FIELD_WRITE_TYPE)
    //		}
    //        }
    //    }
  }

  private def assertEnclosingMethodAndInnerClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    public class EnclosingMethodClass {
    aDeps.assertDependency("test.EnclosingMethodClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.EnclosingMethodClass")
    //  //implicit field definition in the default constructor
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass$1", CREATES)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass$1", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", CALLS_METHOD)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass", USES_PARAMETER_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass.enclosingField", WRITES_FIELD)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
    //  //implicit field definition in the class initialization method
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass$2", CREATES)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass$2", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass$2.<init>()", CALLS_METHOD)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass.staticEnclosingField", WRITES_FIELD)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
    //    
    //        public Object enclosingField = new Object() {
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingField", "test.EnclosingMethodClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingField", "java.lang.Object", IS_OF_TYPE)
    //        };
    aDeps.assertDependency("test.EnclosingMethodClass$1", "java.lang.Object", EXTENDS)
    //	//implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$1.this$0", "test.EnclosingMethodClass$1", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$1.this$0", "test.EnclosingMethodClass", IS_OF_TYPE)
    //	//implicit constructor:
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$1", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
    //	// write to implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$1", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$1.this$0", WRITES_FIELD)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", USES_FIELD_WRITE_TYPE)
    //
    //        public static Object staticEnclosingField = new Object() {
    aDeps.assertDependency("test.EnclosingMethodClass.staticEnclosingField", "test.EnclosingMethodClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass.staticEnclosingField", "java.lang.Object", IS_OF_TYPE)
    //        };
    aDeps.assertDependency("test.EnclosingMethodClass$2", "java.lang.Object", EXTENDS)
    //	//implicit constructor:
    aDeps.assertDependency("test.EnclosingMethodClass$2.<init>()", "test.EnclosingMethodClass$2", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$2.<init>()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$2.<init>()", "java.lang.Object.<init>()", CALLS_METHOD)
    //
    //        public void enclosingMethod() {
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass", IS_DEFINED_IN)
    //    	new Object() {
    aDeps.assertDependency("test.EnclosingMethodClass$3", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.EnclosingMethodClass$3", "test.EnclosingMethodClass.enclosingMethod()", IS_DEFINED_IN)
    //	//implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$3.this$0", "test.EnclosingMethodClass$3", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$3.this$0", "test.EnclosingMethodClass", IS_OF_TYPE)
    //	//implicit constructor:
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$3", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", HAS_PARAMETER_OF_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
    //	// write to implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$3", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$3.this$0", WRITES_FIELD)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", USES_FIELD_WRITE_TYPE)
    //    	    public void innerMethod() {
    aDeps.assertDependency("test.EnclosingMethodClass$3.innerMethod()", "test.EnclosingMethodClass$3", IS_DEFINED_IN)
    //    	    }
    //    	}.innerMethod();
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3", CREATES)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", CALLS_METHOD)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass", USES_PARAMETER_TYPE) // method parameter

    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3.innerMethod()", CALLS_METHOD)
    //        }
    //    }
  }

  private def assertExceptionTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.util.FormatterClosedException;
    //    
    //    import javax.naming.OperationNotSupportedException;
    //    
    //    public class ExceptionTestClass {
    aDeps.assertDependency("test.ExceptionTestClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.ExceptionTestClass")
    //    
    //        public void testMethod() throws IllegalStateException,
    //    	    OperationNotSupportedException {
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "test.ExceptionTestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.lang.IllegalStateException", THROWS)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "javax.naming.OperationNotSupportedException", THROWS)
    //    	throw new FormatterClosedException();
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", CREATES)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException.<init>()", CALLS_METHOD)
    //        }
    //
    //        public void catchMethod() {
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "test.ExceptionTestClass", IS_DEFINED_IN)
    //    	try {
    //    	    try {
    //    		testMethod();
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "test.ExceptionTestClass", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "test.ExceptionTestClass.testMethod()", CALLS_METHOD)
    //    	    } catch (IllegalStateException e) {
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", CATCHES)
    //    	    }
    //    	} catch (Exception e) {
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Exception", CATCHES)
    //    	} finally{
    //    	    Integer.valueOf(42);
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
    //TODO: check if multi-dependencies from finally blocks can be eliminated!
    // The next six dependencies result from required special handling of the finally block
    // Depending on the way the finally block were reached it has to throw an Exception or return normally
    // Hence, the bytecode contains the  finally block three times.
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
    //    	}
    //        }
    //    }
  }

  private def assertTestAnnotation(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.lang.annotation.ElementType;
    //    
    //    public @interface TestAnnotation {
    aDeps.assertDependency("test.TestAnnotation", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.TestAnnotation", "java.lang.annotation.Annotation", IMPLEMENTS)
    //        public abstract String stringValue() default "default";
    aDeps.assertDependency("test.TestAnnotation.stringValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.stringValue()", "java.lang.String", RETURNS)
    //    
    //        public abstract Class<?> classValue() default String.class;
    aDeps.assertDependency("test.TestAnnotation.classValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.classValue()", "java.lang.Class", RETURNS)
    aDeps.assertDependency("test.TestAnnotation.classValue()", "java.lang.String", USES_DEFAULT_CLASS_VALUE_TYPE)
    //    
    //        public abstract ElementType enumValue() default ElementType.TYPE;
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", RETURNS)
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", USES_DEFAULT_ENUM_VALUE_TYPE)
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "java.lang.annotation.ElementType.TYPE", USES_ENUM_VALUE)
    //    
    //        public abstract SuppressWarnings annotationValue() default @SuppressWarnings("default");
    aDeps.assertDependency("test.TestAnnotation.annotationValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", RETURNS)
    aDeps.assertDependency("test.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", USES_DEFAULT_ANNOTATION_VALUE_TYPE)
    //    
    //        public abstract Class<?>[] arrayClassValue() default { String.class,
    //    	    Integer.class };
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "test.TestAnnotation", IS_DEFINED_IN)
    //TODO: check this...since only the underlying types of arrays are considered, the return type has to be checked to be java.lang.Class 
    //aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.Class[]", RETURNS)
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.Class", RETURNS)
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.String", USES_DEFAULT_CLASS_VALUE_TYPE)
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.Integer", USES_DEFAULT_CLASS_VALUE_TYPE)
    //    }
  }

  private def assertAnnotationDefaultAttributeTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.lang.annotation.ElementType;
    //    
    //    @TestAnnotation
    //    public class AnnotationDefaultAttributeTestClass {
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass", "test.TestAnnotation", ANNOTATED_WITH)
    assertImplicitDefaultConstructor("test.AnnotationDefaultAttributeTestClass")
    //    
    //        @TestAnnotation(stringValue = "noDefault", classValue = Integer.class, enumValue = ElementType.METHOD, annotationValue = @SuppressWarnings("noDefault"), arrayClassValue = {
    //    	    Long.class, Boolean.class })
    //        public void testMethod() {
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "test.AnnotationDefaultAttributeTestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "test.TestAnnotation", ANNOTATED_WITH)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Integer", USES_DEFAULT_CLASS_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType", USES_DEFAULT_ENUM_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType.METHOD", USES_ENUM_VALUE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.SuppressWarnings", USES_DEFAULT_ANNOTATION_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Long", USES_DEFAULT_CLASS_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Boolean", USES_DEFAULT_CLASS_VALUE_TYPE)
    //        }
    //    }
  }

  private def assertInstructionsTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.io.FilterInputStream;
    //    import java.io.InputStream;
    //    import java.util.zip.InflaterInputStream;
    //    import java.util.zip.ZipInputStream;
    //    
    //    public class InstructionsTestClass {
    aDeps.assertDependency("test.InstructionsTestClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.InstructionsTestClass")
    //        public Object field;
    aDeps.assertDependency("test.InstructionsTestClass.field", "test.InstructionsTestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.InstructionsTestClass.field", "java.lang.Object", IS_OF_TYPE)
    //        public static InputStream staticField;
    aDeps.assertDependency("test.InstructionsTestClass.staticField", "test.InstructionsTestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.InstructionsTestClass.staticField", "java.io.InputStream", IS_OF_TYPE)
    //    
    //        public void method() {
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass", IS_DEFINED_IN)
    //    	// NEW and INVOKESPECIAL (constructor call)
    //    	Object obj = new Object();
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object", CREATES)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object.<init>()", CALLS_METHOD)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    //    	FilterInputStream stream = null;
    //    	// ANEWARRAY
    //    	obj = new Long[1];
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Long", CREATES_ARRAY_OF_TYPE)
    //    	// MULTIANEWARRAY
    //    	obj = new Integer[1][];
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Integer", CREATES_ARRAY_OF_TYPE)
    //    
    //    	// PUTFIELD
    //    	field = obj;
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass.field", WRITES_FIELD)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
    //    	// GETFIELD
    //    	obj = field;
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass.field", READS_FIELD)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object", USES_FIELD_READ_TYPE)
    //    	// INSTANCEOF
    //    	if (obj instanceof ZipInputStream) {
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.util.zip.ZipInputStream", CHECKS_INSTANCEOF)
    //    	    // CHECKCAST
    //    	    stream = (InflaterInputStream) obj;
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.util.zip.InflaterInputStream", CASTS_INTO)
    //    	    // PUTSTATIC
    //    	    staticField = stream;
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass.staticField", WRITES_FIELD)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.io.InputStream", USES_FIELD_WRITE_TYPE)
    //    	    // GETSTATIC
    //    	    obj = staticField;
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.InstructionsTestClass.staticField", READS_FIELD)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.io.InputStream", USES_FIELD_READ_TYPE)
    //    	}
    //    
    //    	// INVOKESTATIC
    //    	System.currentTimeMillis();
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.System", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.System.currentTimeMillis()", CALLS_METHOD)
    //    
    //    	TestInterface ti = new TestClass();
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.TestClass", CREATES)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.TestClass", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.TestClass.<init>()", CALLS_METHOD)
    //    	// INVOKEINTERFACE
    //    	ti.testMethod();
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.TestInterface", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "test.TestInterface.testMethod()", CALLS_INTERFACE_METHOD)
    //    
    //    	// INVOKEVIRTUAL
    //    	obj.equals(stream);
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object.equals(java.lang.Object)", CALLS_METHOD)
    aDeps.assertDependency("test.InstructionsTestClass.method()", "java.lang.Object", USES_PARAMETER_TYPE)
    //    
    //    	// TODO: add test for INVOKEDYNAMIC
    //        }
    //    }
  }

  private def assertImplicitDefaultConstructor(className: String)(implicit aDeps: AssertableDependencies) {
    //	//implicit constructor:
    aDeps.assertDependency(className + ".<init>()", className, IS_DEFINED_IN)
    aDeps.assertDependency(className + ".<init>()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
    aDeps.assertDependency(className + ".<init>()", "java.lang.Object.<init>()", CALLS_METHOD)
  }

  class AssertableDependencies(var deps: Dependencies) {
    def assertDependency(src: String, trgt: String, dType: DependencyType) {
      val dep = (src, trgt, dType)
      if (deps.contains(dep)) {
        deps = deps diff List(dep)
        //        println("verified dependency: " + src + "--[" + dType + "]-->" + trgt)
      } else {
        throw new AssertionError("Dependency " + dep + " was not extracted successfully!\nRemaining dependencies:\n" + deps.mkString("\n"))
      }
    }
  }

}
