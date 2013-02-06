
.SUFFIXES: .scala .class .jar .html
.PHONY: pics

SC=fsc
SCFLAGS=
#SCFLAGS=-unchecked -g:vars

# ImageMagick conversion program:
CONVERT=convert

SRC=grid.scala display.scala

JARFILE=SwineMeeper.jar
MANIFEST=SwineMeeper.mf

all: $(JARFILE)

# Creates the JAR file but also creates symlinks to the source code
# next to the .class files so that jdb can find them.
$(JARFILE): $(SRC) pics
	$(SC) $(SCFLAGS) $(SRC)
	cp pics/*.png ca/blit/SwineMeeper/
	jar -cfm $(JARFILE) $(MANIFEST) ca/
#   Uncomment the following (and delete these lines) if your debugger
#   needs the source tree to match the module heirarchy:
#	(for i in $(SRC); do ln -sf `pwd`/$$i ca/blit/SwineMeeper/`basename $$i`; done)

pics:
	(cd pics/masters && for i in *.png; do convert $$i -scale 16x16 ../$$i; done)

clean:
	-rm $(JARFILE)

tidy: clean
	-rm -rf ca/ com/ pics/*.png index.html index.js package.html index/ lib/

doc:
	scaladoc $(SRC)

index.html: $(JARFILE)
	scaladoc $(MYSRC)

run: $(JARFILE)
	scala SwineMeeper.jar



