# Z80TestZex  
## Launcher for the Frank Cringle's Z80 Instruction Set Exerciser
---
### MIT License  

Copyright (c) 2019 Nicolas Allemand  
Copyright (c) 2025 Jose Andres Calvo Conde

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.  

---  
---  
### NOTES

**About the code**  
This is a porting from C99 to Java for code https://github.com/superzazu/z80/blob/master/z80_tests.c by Nicolas Allemand (superzazu) to launch Frank Cringle's Z80 Instruction Set Exerciser.  
Frank Cringle's Z80 Instruction Set Exerciser is intended to run in a CP/M system. To avoid the CP/M dependency, the test launcher by superzazu intercept CP/M requests to addresses 0h and 5h for stop the tests and print characters on screen. 
Z80TestZex implements custom code at addresses 0h and 5h to perform these tasks without traps.

**About Z80 Instruction Set Exerciser, by Frank D. Cringle**  
Frank Cringle's Z80 Instruction Set Exerciser attempts to execute every Z80 opcode, putting them through a cycle of tests and comparing the results to actual results from running the code on a real Z80. The exerciser is supplied with Frank's Yaze (Yet Another Z80 Emulator). It is often difficult to track down, so Jonathan Graham Harston (https://mdfs.net/User/JGH) put it together here (https://mdfs.net/Software/Z80/Exerciser/), as well as some conversions (https://mdfs.net/Software/Z80/Exerciser/Spectrum). The latest release of Yaze is available at Andreas Gerlich's website (https://www.mathematik.uni-ulm.de/users/ag/yaze-ag).  

ZEXDOC only tests officially documented flag effects, whereas ZEXALL tests all flags changes, compared against test on real hardware. 

**About ROMs for Z80 Instruction Set Exerciser, by Frank D. Cringle**  
ZEXALL and ZEXDOC are released under the GNU General Public License. 
