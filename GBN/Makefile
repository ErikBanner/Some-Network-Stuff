JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = \
		Sender.java \
		Receiver.java \
		packet.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class seqnum.log ack.log arrival.log