all:
	javac Main.java
clean:
	rm -f *.class
	rm -rf client
	rm -rf server
fclean:
	rm -f *.class
	rm -rf client
	rm -rf server
	rm -f *.txt