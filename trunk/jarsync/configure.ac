dnl Process this file with autoconf to produce a configure script.
dnl $Id$

AC_INIT(jarsync, 0.0.4)

AC_SUBST(COMPILER)
AC_SUBST(TEST_CFLAGS)
AC_SUBST(CFLAGS)
AC_SUBST(JAR)
AC_SUBST(COMPRESS)
AC_SUBST(COMPRESS_SUFFIX)
AC_SUBST(JFLEX)
AC_SUBST(CUP)
AC_PROG_MAKE_SET

dnl --with arguments
AC_ARG_WITH(jikes,
	[  --with-jikes            Jikes compiler will be used ],
	use_jikes="yes", use_jikes="no")
AC_ARG_WITH(gcj,
	[  --with-gcj              GCJ compiler will be used ],
	use_gcj="yes", use_gcj="no")
AC_ARG_WITH(compress-prog,
	[  --with-compress-prog=PROG   Use compression program PROG (gzip)],
	[compress_prog=$withval])
AC_ARG_WITH(compress-suffix,
	[  --with-compress-suffix=SUF  Use SUF for compressed files (gz)],
	[compress_suffix=$withval])

dnl Check which Java compiler to use.
if test $use_jikes = "yes"; then
	AC_PATH_PROG(COMPILER, jikes, no)
	CFLAGS="-g -classpath $CLASSPATH:../classes -d ../classes"
	TEST_CFLAGS="-g -classpath $CLASSPATH:lib/jarsync.jar"
elif test $use_gcj = "yes"; then
	AC_PATH_PROG(COMPILER, gcj, no)
	CFLAGS="-C -g -fCLASSPATH=$CLASSPATH:../classes -d ../classes"
	TEST_CFLAGS="-C -g -fCLASSPATH=$CLASSPATH:lib/jarsync.jar"
else
	AC_PATH_PROG(COMPILER, javac, no)
	CFLAGS="-g -classpath $CLASSPATH:../classes -d ../classes"
	TEST_CFLAGS="-g -classpath $CLASSPATH:lib/jarsync.jar"
fi

if test $COMPILER = "no"; then
	AC_MSG_ERROR([No Java compiler found.])
fi

AC_PATH_PROG(JAR, jar, no)

if test $JAR = "no"; then
	AC_MSG_ERROR([No Java archiver found.])
fi

if test -z "$compress_prog"; then
	compress_prog=gzip
fi

if test $compress_prog = "gzip" -a -z "$COMPRESS_SUFFIX"; then
	COMPRESS_SUFFIX=gz
elif test $compress_prog = "bzip2" -a -z "$COMPRESS_SUFFIX"; then
	COMPRESS_SUFFIX=bz2
fi

AC_PATH_PROG(JFLEX, jflex, no)
if test "x${JFLEX}" = xno; then
	AC_MSG_WARN([No jflex found. `make vwdiff' will not work])
fi

AC_PATH_PROG(CUP, cup, no)
if test "x${cup}" = xno; then
	AC_MSG_WARN([No cup found. `make vwdiff' will not work])
fi

AC_PATH_PROG(COMPRESS, $compress_prog, no)
if test $COMPRESS = "no"; then 
	AC_MSG_WARN([no `$compress_prog' found, this will break `make dist'])
	unset COMPRESS
fi

AC_OUTPUT(Makefile source/Makefile jzlib/Makefile getopt/Makefile java_cup/Makefile)
