rem
rem This script lets you run your application without the wrapper.  Useful for testing.
rem
java -Xms16m -Xmx64m -Djava.library.path="../lib" -Djava.class.path="../lib/wrapper.jar;../lib/wrappertest.jar" -Dwrapper.native_library="wrapper" -Dwrapper.debug="TRUE" org.tanukisoftware.wrapper.test.Main

