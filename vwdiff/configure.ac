dnl Process this file with autoconf to produce configure.

AC_INIT(vwdiff, 0.1, rsdio@metastatic.org)
AM_INIT_AUTOMAKE(dist-zip)
AC_CONFIG_SRCDIR(source/org/metastatic/vwdiff/Main.java)

dnl Checks for programs.

AC_PROG_JAVA
AC_PROG_JAVAC
AC_CHECK_CLASSPATH
AC_CHECK_PATH_SEPARATOR
AC_CHECK_FILE_SEPARATOR
AC_PATH_PROG(JAR, jar)
AC_SUBST(JAR)
AC_PATH_PROG(SHELL, sh)
AC_SUBST(SHELL)

dnl Checks for classes.
AC_CHECK_CLASS(org.metastatic.rsync.Configuration, ,
	AC_MSG_ERROR([Jarsync library not found]))
AC_CHECK_CLASS(org.apache.log4j.Logger, ,
	AC_MSG_ERROR([Log4j is not available.]))

mkdir classes

AC_OUTPUT([
  Makefile
  bin/Makefile
  bin/vwdiff.sh
  etc/Makefile
  source/Makefile
  source/org/metastatic/vwdiff/version.java
])
