all:
	javac Client.java
	javac FileServer.java
clean:
	rm -f *.class
	rm -rf client/*
fclean:
	rm -f *.class
	rm -rf client/*
	rm -r A/*
	rm -r B/*