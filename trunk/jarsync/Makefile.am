## Process this file with automake to produce Makefile.in.

JAVADOC = @JAVADOC@
BREAKITERATOR = @BREAKITERATOR@

EXTRA_DIST = LICENSE jarsyncd rdiff rdiff.magic getopt/COPYING
SUBDIRS = source test

docs: FORCE
	-mkdir $(srcdir)/apidocs
	$(JAVADOC) $(BREAKITERATOR) -d $(srcdir)/apidoc \
	  -sourcepath $(srcdir)/source/:$(srcdir)/getopt/ \
	  -windowtitle "Jarsync $(VERSION)$(PRE_VERSION) documentation" \
	  -doctitle "Jarsync $(VERSION)$(PRE_VERSION)" \
	  -link http://java.sun.com/j2se/1.4.1/docs/api/package-list \
	  -group "Jarsync API" org.metastatic.rsync \
	  -group "Jarsync Client and Server" org.metastatic.rsync.v2 \
	  -group "Java Getopt" gnu.getopt org.metastatic.rsync \
	  org.metastatic.rsync.v2 gnu.getopt

FORCE:
