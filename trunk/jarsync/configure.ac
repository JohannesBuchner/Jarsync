dnl Process this file with autoconf to produce a configure script.
dnl $Id$

AC_INIT(jarsync, 0.2, rsdio@metastatic.org)
AC_CONFIG_SRCDIR(source/org/metastatic/rsync/Configuration.java)

AC_PROG_JAVA
AC_PROG_JAVAC
AC_PROG_MAKE_SET
AC_CHECK_CLASSPATH
AC_CHECK_PATH_SEPARATOR
AC_CHECK_FILE_SEPARATOR

PRE_VERSION='pre1'
AC_SUBST(PRE_VERSION)

AC_SUBST(JAVAFLAGS)
AC_SUBST(JAVACFLAGS)
AC_SUBST(COMPRESS)
AC_SUBST(SHELL)
AC_SUBST(JAR)
AC_SUBST(JAVA)
AC_SUBST(JAVAC)
AC_SUBST(MAKE_CLIENT)

dnl --with and --enable arguments
AC_ARG_ENABLE(client, AC_HELP_STRING([--enable-client],
	[Build rsync-compatible program]),
	[make_client=yes], [make_client=no])
AC_ARG_WITH(log4j-jar, AC_HELP_STRING([--with-log4j-jar=PATH],
	[Specify the location of log4j jar]),
	[log4j_jar=$withval], [log4j_jar=no])
AC_ARG_WITH(javaunix, AC_HELP_STRING([--with-javaunix-jar],
	[Specify the location of the JavaUnix jar]),
	[javaunix_jar=$withval], [javaunix_jar=no])

CLASSPATH=$CLASSPATH${PATH_SEPARATOR}..${FILE_SEPARATOR}classes
if test $log4j_jar != "yes" -a $log4j_jar != "no"; then
   CLASSPATH=${CLASSPATH}${PATH_SEPARATOR}${log4j_jar}
fi

AC_CHECK_CLASS(org.apache.log4j.Logger, [have_log4j=yes], [have_log4j=no])
AC_CHECK_CLASS(javaunix.io.UnixFile, [have_javaunix=yes], [have_javaunix=no])

AC_MSG_CHECKING([if we are building the client program.])
if test "x$make_client" = "xyes"; then
   if test "x$have_log4j" = "xno"; then
      AC_MSG_RESULT(error)
      AC_MSG_ERROR([Log4j is required for --enable-client.])
   fi
   if test "x$have_javaunix" = "xno"; then
      AC_MSG_RESULT(error)
      AC_MSG_ERROR([JavaUnix is required for --enable-client.])
   fi
   AC_MSG_RESULT(yes)
   MAKE_CLIENT='client'
else
   AC_MSG_RESULT(no)
   MAKE_CLIENT='.DUMMY'
fi

dnl Check which Java compiler to use.
case $JAVAC in
   *jikes|*javac)
      JAVACFLAGS="-classpath $CLASSPATH -d ../classes $JAVACFLAGS"
      ;;
   *gcj)
      JAVACFLAGS="--classpath=$CLASSPATH -d ../classes $JAVACFLAGS"
      ;;
esac

JAVAFLAGS="$JAVAFLAGS -classpath $CLASSPATH"

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

AC_OUTPUT([
  Makefile
  getopt/Makefile
  source/Makefile
  source/org/metastatic/rsync/version.java
  test/Makefile
  test/gnu/testlet/config.java
])
