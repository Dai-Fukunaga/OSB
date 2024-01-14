all:
	javac Client.java
	javac FileServer.java
clean:
	rm -f *.class
	rm -rf server
fclean:
	rm -f *.class
	rm -rf client/*
	rm -rf server
	rm -f *.txt