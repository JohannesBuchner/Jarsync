## Process this file with automake to create Makefile.in.

JAR = @JAR@
JAVAC = @JAVAC@
JAVACFLAGS = @JAVACFLAGS@

main_jar = ../vwdiff.jar
BUILT_SOURCES = $(main_jar)
data_DATA = $(main_jar)
EXTRA_DIST = $(sources)

sources = \
  $(srcdir)/com/keypoint/PngEncoder.java \
  $(srcdir)/com/keypoint/PngEncoderB.java \
  $(srcdir)/org/metastatic/vwdiff/Config.java \
  $(srcdir)/org/metastatic/vwdiff/HTTPException.java \
  $(srcdir)/org/metastatic/vwdiff/HTTPRequest.java \
  $(srcdir)/org/metastatic/vwdiff/HTTPResponse.java \
  $(srcdir)/org/metastatic/vwdiff/Target.java \
  $(srcdir)/org/metastatic/vwdiff/Main.java \
  $(srcdir)/org/metastatic/vwdiff/version.java

classes = $(patsubst $(srcdir)/%.java,../classes/%.class,$(sources))

$(main_jar): $(classes)
	$(JAR) cmf $(top_srcdir)/etc/vwdiff.mf $(main_jar) -C ../classes com -C ../classes org

$(classes): ../classes/%.class: $(srcdir)/%.java
	$(JAVAC) $(JAVACFLAGS) -d ../classes $<
