@echo off
if "%1" == "" goto error_msg
java -cp target;target\aux_classes %1
goto end
:error_msg
echo ERROR: Main class to execute needed as argument.
echo Valid classes: Z80TestZex
:end