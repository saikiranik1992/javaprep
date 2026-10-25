# Zero to Hero Technical Architect Interview Portal (Spring Boot + Thymeleaf + 19 PDF Notebooks + Live Java IDE)

Production-ready **Spring Boot 3.5 + Thymeleaf** interview preparation platform featuring **19 Premium PDF Notebooks (2,917 Pages · 2,280 Curated Q&A)**, a **Daylight Paper Light Theme with Dark Text**, **Wrapped Topic & Chapter Navigation (No Scrollbars)**, and a **Live Java & Spring Boot IDE Sandbox** compatible with OpenJDK 17, 21, 23, 24, and 25.

## Key Features
1. **19 Dedicated PDF Notebook Tabs (2,917 Pages · 2,280 Q&A)**:
   - `Core_Java_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (232 pages · 182 Q&A + 24 Labs)
   - `Java_8_11_17_21_25_Clean_Premium_Notes.pdf` (179 pages · 126 Q&A + 20 Labs)
   - `Java_Threads_Handwritten_Premium_Notes.pdf` (114 pages · 80 Q&A + 10 Labs)
   - `Java_Memory_Model_Handwritten_Premium_Notes.pdf` (126 pages · 80 Q&A + 8 Labs)
   - `JVM_GC_Performance_Handwritten_Premium_Notes.pdf` (158 pages · 100 Q&A + 10 Labs)
   - `Java_Design_Patterns_Zero_to_Hero_Clean_Premium_Notes.pdf` (150 pages · 130 Q&A + 12 Labs)
   - `Java_Programming_Interviews_Handwritten_Premium_Notes.pdf` (127 pages · 122 Solved Problems)
   - `Spring_Boot_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (198 pages · 180 Q&A + 12 Labs)
   - `Spring_Security_Zero_to_Hero_Clean_Premium_Notes.pdf` (185 pages · 166 Q&A)
   - `Hibernate_Zero_to_Hero_Clean_Premium_Notes.pdf` (142 pages · 100 Q&A + 12 Labs)
   - `JUnit_Mockito_Zero_to_Hero_Clean_Premium_Notes.pdf` (142 pages · 100 Q&A + 12 Labs)
   - `Microservices_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (177 pages · 150 Q&A + 14 Sketches)
   - `Spring_Boot_Kafka_Zero_to_Hero_Clean_Premium_Notes.pdf` (137 pages · 103 Q&A + 10 Labs)
   - `Spring_Boot_Redis_Zero_to_Hero_Clean_Premium_Notes.pdf` (140 pages · 125 Q&A + 10 Labs)
   - `SQL_Zero_to_Hero_Clean_Premium_Notes.pdf` (128 pages · 106 Q&A + 15 Exercises)
   - `Docker_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (142 pages · 110 Q&A + 12 Labs)
   - `Kubernetes_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (163 pages · 110 Q&A + 12 Labs)
   - `AWS_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (146 pages · 100 Q&A + 8 Labs)
   - `Git_Zero_to_Hero_Handwritten_Premium_Notes.pdf` (131 pages · 110 Q&A + 14 Labs)
2. **Light Background with Dark Text & No Topic Scrollbars**:
   - Uses a crisp Daylight Paper light theme (`#F8FAFC` / `#FFFFFF` surfaces with `#0F172A` dark slate text) across all cards, code blocks, and the IDE panel.
   - Multi-row wrapped topic tabs and chapter pills (`flex-wrap`) eliminate horizontal scrollbars.
3. **Compatible Live Java IDE Execution (`/api/ide/execute`)**:
   - Compiles `Main.java` cleanly via `javac -encoding UTF-8 -d <tempDir> Main.java` and executes `java -Dfile.encoding=UTF-8 -cp <tempDir> Main` without hardcoded `--source 25` flags, running seamlessly on OpenJDK 17, 21, 23.0.2, 24, and 25.

## Run Locally
```bash
mvn spring-boot:run
# Open http://localhost:8080
```
