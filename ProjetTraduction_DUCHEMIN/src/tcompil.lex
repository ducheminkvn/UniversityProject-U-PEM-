%{
#include "./tcompil.h"
%}
%option nounput
%option noinput
%%
"/*"[^"/*"]*"*/" ;
[ \t\n]+ ;
[0-9]+ {sscanf(yytext,"%d",&yylval.entier); return NUM;}
"CONST" {yylval.type=2;return CONST;}
"'"."'" {yylval.caractere=yytext[1]; return CARACTERE;}
"entier" {yylval.type=2;return TYPE;}/*int */
"caractere" {yylval.type=1;return TYPE;}/*char */
"if" {return IF;}
"else" {return ELSE;}
"while" {return WHILE;}
"print"	{return PRINT;}
"read" {return READ;}
"readch" {return READCH;}
"main" {return MAIN;}
"void" {return VOID;}
"==" {yylval.instr="EQUAL";return COMP;}
"!=" {yylval.instr="NOTEQ";return COMP;}
"<=" {yylval.instr="LEQ";return COMP;}
">=" {yylval.instr="GEQ";return COMP;}
"<" {yylval.instr="LESS";return COMP;}
">" {yylval.instr="GREATER";return COMP;}
"&&" {yylval.instr="MUL";return BOPE;}
"||" {yylval.instr="ADD";return BOPE;}
";" {return PV;}
"," {return VRG;}
"(" {return LPAR;}
")" {return RPAR;}
"{" {return LACC;}
"}" {return RACC;}
"[" {return LSQB;}
"]" {return RSQB;}
"=" {return EGAL;}
"+" {yylval.instr="ADD"; return ADDSUB;}
"-" {yylval.instr="SUB"; return ADDSUB;}
"*" {yylval.instr="MUL";return DIVSTAR;}
"/" {yylval.instr="DIV";return DIVSTAR;}
"%" {yylval.instr="MOD";return DIVSTAR;}
"!" {return NEGATION;}
"return" {return RETURN;}
[A-Za-z][A-Za-z0-9_]* {sscanf(yytext,"%s",yylval.id);return IDENT;}
%%
