dnl Process this file with autoconf to produce a configure script.
dnl $Id$

AC_INIT(jarsync, 0.2, rsdio@metastatic.org)
AM_INIT_AUTOMAKE
AC_CONFIG_SRCDIR(source/org/metastatic/rsync/Configuration.java)

dnl Checks for programs.

AC_PROG_JAVA
AC_PROG_JAVAC
AC_PROG_JAVADOC
AC_PROG_MAKE_SET
AC_CHECK_CLASSPATH
AC_CHECK_PATH_SEPARATOR
AC_CHECK_FILE_SEPARATOR

PRE_VERSION='pre1'
AC_SUBST(PRE_VERSION)

AC_SUBST(JAVAFLAGS)
AC_SUBST(JAVACFLAGS)
AC_SUBST(CHECKJAVACFLAGS)
AC_SUBST(CFLAGS)
AC_SUBST(COMPRESS)
AC_SUBST(SHELL)
AC_SUBST(JAR)
AC_SUBST(CC)
AC_SUBST(JAVA)
AC_SUBST(JAVAC)
AC_SUBST(JAVADOC)
AC_SUBST(MAKE_CLIENT)

dnl --with and --enable arguments
AC_ARG_ENABLE([client-server], AC_HELP_STRING([--enable-client-server],
	[Build rsync-compatible client/server]),
	[make_v2=yes], [make_v2=no])
AC_ARG_WITH(log4j-jar, AC_HELP_STRING([--with-log4j-jar=PATH],
	[Specify the location of log4j jar]),
	[log4j_jar=$withval], [log4j_jar=no])
AC_ARG_WITH(javaunix, AC_HELP_STRING([--with-javaunix-jar],
	[Specify the location of the JavaUnix jar]),
	[javaunix_jar=$withval], [javaunix_jar=no])

dnl Append extra jars to CLASSPATH
CLASSPATH=$CLASSPATH${PATH_SEPARATOR}..${FILE_SEPARATOR}classes
if test $log4j_jar != "yes" -a $log4j_jar != "no"; then
   CLASSPATH=${CLASSPATH}${PATH_SEPARATOR}${log4j_jar}
fi
if test $javaunix_jar != "yes" -a $javaunix_jar != "no"; then
   CLASSPATH=${CLASSPATH}${PATH_SEPARATOR}${javaunix_jar}
fi

dnl Checks for classes.

AC_CHECK_CLASS(org.apache.log4j.Logger, [have_log4j=yes], [have_log4j=no])
AC_CHECK_CLASS(javaunix.io.UnixFile, [have_javaunix=yes], [have_javaunix=no])
AC_CHECK_CLASS(java.nio.channels.SocketChannel, [have_nio=yes], [have_nio=no])

AM_CONDITIONAL(HAVE_NIO, test "x$have_nio" = "xyes")

AC_MSG_CHECKING([if we are building the client and server programs.])
if test "x$make_v2" = "xyes"; then
   if test "x$have_log4j" = "xno"; then
      AC_MSG_RESULT(error)
      AC_MSG_ERROR([Log4j is required for --enable-client-server.])
   fi
   if test "x$have_javaunix" = "xno"; then
      AC_MSG_RESULT(error)
      AC_MSG_ERROR([JavaUnix is required for --enable-client-server.])
   fi
   AC_MSG_RESULT(yes)
else
   AC_MSG_RESULT(no)
fi

AM_CONDITIONAL(MAKE_V2, test "x$make_v2" = "xyes")dnl

dnl Check which Java compiler to use.
case $JAVAC in
   *jikes|*javac)
      CHECKJAVACFLAGS="-classpath $CLASSPATH:classes:\$(srcdir) $JAVACFLAGS"
      JAVACFLAGS="-classpath $CLASSPATH:\$(srcdir) $JAVACFLAGS"
      ;;
   *gcj)
      CHECKJAVACFLAGS="-C --classpath=$CLASSPATH:classes:\$(srcdir) $JAVACFLAGS"
      JAVACFLAGS="-C --classpath=$CLASSPATH:\$(srcdir) $JAVACFLAGS"
      ;;
esac

JAVAFLAGS="$JAVAFLAGS -classpath $CLASSPATH:classes"

AC_PATH_PROG(JAR, jar, no)
if test $JAR = "no"; then
	AC_MSG_ERROR([No Java archiver found.])
fi

compress_prog=gzip
if test $compress_prog = "gzip" -a -z "$COMPRESS_SUFFIX"; then
	COMPRESS_SUFFIX=gz
elif test $compress_prog = "bzip2" -a -z "$COMPRESS_SUFFIX"; then
	COMPRESS_SUFFIX=bz2
fi
AC_PATH_PROG(COMPRESS, $compress_prog, [no])
if test $COMPRESS = "no"; then 
	AC_MSG_WARN([no `$compress_prog' found, this will break `make dist'])
	unset COMPRESS
fi

AC_PATH_PROG(SHELL, [sh],   [no])

dnl  For Mauve.
TMPDIR=/tmp
CHECK_PATH_SEPARATOR=$PATH_SEPARATOR
CHECK_FILE_SEPARATOR=$FILE_SEPARATOR
AC_SUBST(SRCDIR)
AC_SUBST(TMPDIR)
AC_SUBST(CHECK_PATH_SEPARATOR)
AC_SUBST(CHECK_FILE_SEPARATOR)

mkdir classes
mkdir lib

AC_OUTPUT([
  Makefile
  source/Makefile
  source/org/metastatic/rsync/version.java
  test/Makefile
  test/gnu/testlet/config.java
])
