CC=gcc
CFLAGS=-Wall
LDFLAGS=-Wall -lfl
EXEC=tcompil

all: clean $(EXEC)

$(EXEC): 
	cd ./src/ && $(MAKE)
	mv ./src/tcompil .

clean:
	rm -f $(EXEC)
