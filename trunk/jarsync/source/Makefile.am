# Makefile for Jarsync.
# $Id$

JAR = @JAR@
JAVAC = @JAVAC@
JAVACFLAGS = @JAVACFLAGS@

main_jar = ../lib/$(PACKAGE).jar
BUILT_SOURCES = $(main_jar)
data_DATA = $(main_jar)
EXTRA_DIST = $(sources) $(properties) \
  $(srcdir)/org/metastatic/rsync/version.java.in \
  $(srcdir)/org/metastatic/rsync/package.html \
  $(srcdir)/org/metastatic/rsync/v2/package.html

if HAVE_NIO
nio_sources = \
  org/metastatic/rsync/v2/BufferFileList.java \
  org/metastatic/rsync/v2/BufferSender.java \
  org/metastatic/rsync/v2/BufferTool.java \
  org/metastatic/rsync/v2/BufferUtil.java \
  org/metastatic/rsync/v2/DuplexByteBuffer.java \
  org/metastatic/rsync/v2/NonblockingDaemon.java \
  org/metastatic/rsync/v2/Protocol.java \
  org/metastatic/rsync/v2/RsyncBufferAppender.java
else
nio_sources =
endif

if MAKE_V2
client_sources = \
  org/metastatic/rsync/v2/BlockingDaemon.java \
  org/metastatic/rsync/v2/ConsoleClient.java \
  org/metastatic/rsync/v2/Constants.java \
  org/metastatic/rsync/v2/Daemon.java \
  org/metastatic/rsync/v2/FNMatch.java \
  org/metastatic/rsync/v2/FileInfo.java \
  org/metastatic/rsync/v2/FileList.java \
  org/metastatic/rsync/v2/Glob.java \
  org/metastatic/rsync/v2/Module.java \
  org/metastatic/rsync/v2/MultiplexedIO.java \
  org/metastatic/rsync/v2/MultiplexedInputStream.java \
  org/metastatic/rsync/v2/MultiplexedOutputStream.java \
  org/metastatic/rsync/v2/Options.java \
  org/metastatic/rsync/v2/ProtocolException.java \
  org/metastatic/rsync/v2/Receiver.java \
  org/metastatic/rsync/v2/RsyncAppender.java \
  org/metastatic/rsync/v2/RsyncdConf.java \
  org/metastatic/rsync/v2/Sender.java \
  org/metastatic/rsync/v2/SocketClient.java \
  org/metastatic/rsync/v2/Statistics.java \
  org/metastatic/rsync/v2/StatsModule.java \
  $(nio_sources)

jarclasses = \
  org/metastatic/rsync/*.class \
  org/metastatic/rsync/v2/*.class \
  gnu/getopt/*.class \
  gnu/getopt/*.properties
else
client_sources =

jarclasses = \
  org/metastatic/rsync/*.class \
  gnu/getopt/*.class \
  gnu/getopt/*.properties
endif

sources = \
  org/metastatic/rsync/BrokenMD4.java \
  org/metastatic/rsync/Checksum32.java \
  org/metastatic/rsync/ChecksumPair.java \
  org/metastatic/rsync/Configuration.java \
  org/metastatic/rsync/DataBlock.java \
  org/metastatic/rsync/Delta.java \
  org/metastatic/rsync/GeneratorEvent.java \
  org/metastatic/rsync/GeneratorListener.java \
  org/metastatic/rsync/GeneratorStream.java \
  org/metastatic/rsync/Generator.java \
  org/metastatic/rsync/JarsyncProvider.java \
  org/metastatic/rsync/MD4.java \
  org/metastatic/rsync/Matcher.java \
  org/metastatic/rsync/MatcherEvent.java \
  org/metastatic/rsync/MatcherListener.java \
  org/metastatic/rsync/MatcherStream.java \
  org/metastatic/rsync/Offsets.java \
  org/metastatic/rsync/ParameterException.java \
  org/metastatic/rsync/ParameterListener.java \
  org/metastatic/rsync/Parameters.java \
  org/metastatic/rsync/Rdiff.java \
  org/metastatic/rsync/RebuilderEvent.java \
  org/metastatic/rsync/RebuilderListener.java \
  org/metastatic/rsync/RebuilderStream.java \
  org/metastatic/rsync/Rebuilder.java \
  org/metastatic/rsync/RollingChecksum.java \
  org/metastatic/rsync/TwoKeyMap.java \
  org/metastatic/rsync/Util.java \
  gnu/getopt/Getopt.java \
  gnu/getopt/LongOpt.java \
  $(client_sources)

properties = \
  $(srcdir)/gnu/getopt/MessagesBundle.properties \
  $(srcdir)/gnu/getopt/MessagesBundle_cs.properties \
  $(srcdir)/gnu/getopt/MessagesBundle_de.properties \
  $(srcdir)/gnu/getopt/MessagesBundle_fr.properties \
  $(srcdir)/gnu/getopt/MessagesBundle_nl.properties \
  $(srcdir)/gnu/getopt/MessagesBundle_no.properties

classes = $(patsubst %.java,../classes/%.class,$(sources))

$(main_jar): $(classes) ../classes/org/metastatic/rsync/version.class
	cp $(properties) ../classes/gnu/getopt/
	cd ../classes && $(JAR) cf $(main_jar) $(jarclasses)

../classes/org/metastatic/rsync/version.class: org/metastatic/rsync/version.java
	$(JAVAC) $(JAVACFLAGS) -d ../classes $<

$(classes): ../classes/%.class: $(srcdir)/%.java
	$(JAVAC) $(JAVACFLAGS) -d ../classes $<

clean-local:
	rm -f $(classes) ../classes/org/metastatic/rsync/version.class
