## Process this file with automake to produce Makefile.in.

JAVADOC = @JAVADOC@
BREAKITERATOR = @BREAKITERATOR@

EXTRA_DIST = LICENSE rdiff rdiff.magic COPYING.getopt ChangeLog.getopt \
  README.getopt
SUBDIRS = source test

docs: FORCE
	$(JAVADOC) $(BREAKITERATOR) -d apidoc \
	  -sourcepath $(srcdir)/source/:$(srcdir)/getopt/ \
	  -windowtitle "Jarsync $(VERSION)$(PRE_VERSION) documentation" \
	  -doctitle "Jarsync $(VERSION)$(PRE_VERSION)" \
	  -link http://java.sun.com/j2se/1.4.1/docs/api/ \
	  -group "Jarsync API" org.metastatic.rsync \
	  -group "Jarsync Client and Server" org.metastatic.rsync.v2 \
	  -group "Java Getopt" gnu.getopt org.metastatic.rsync \
	  org.metastatic.rsync.v2 gnu.getopt

FORCE:
