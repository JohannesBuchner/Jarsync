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

dnl --with arguments
AC_ARG_WITH([jarsync-jar], AC_HELP_STRING([--with-jarsync-jar=PATH],
	[Specify location of Jarsync library jar.]),
	[jarsync_jar=$withval], [jarsync_jar=no])

dnl Checks for classes.
if test "x$jarsync_jar" = "yes" -o "x$jarsync_jar" = "no"
then
	AC_MSG_ERROR([You must specify the path to jarsync.jar])
fi
CLASSPATH=${CLASSPATH}${PATH_SEPARATOR}${jarsync_jar}
AC_CHECK_CLASS(org.metastatic.rsync.Configuration, [have_jarsync=yes],
	AC_MSG_ERROR([Jarsync library not found]))
JARSYNC_JAR=$jarsync_jar
AC_SUBST(JARSYNC_JAR)

mkdir classes

AC_OUTPUT([
  Makefile
  bin/vwdiff.sh
  source/Makefile
  source/org/metastatic/vwdiff/version.java
])
