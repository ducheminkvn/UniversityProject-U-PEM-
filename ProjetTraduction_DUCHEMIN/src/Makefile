CC=gcc
CFLAGS=-Wall
LDFLAGS=-Wall -lfl
EXEC=tcompil

all: $(EXEC) clean

$(EXEC): $(EXEC).o lex.yy.o
	gcc  -o $@ $^ $(LDFLAGS)

$(EXEC).c: $(EXEC).y
	bison -d -o $@ $^ -v

$(EXEC).h: $(EXEC).c

lex.yy.c: $(EXEC).lex $(EXEC).h
	flex $<

%.o: %.c
	gcc -o $@ -c $< $(CFLAGS)

clean:
	rm -f *.o lex.yy.c $(EXEC).[ch]
