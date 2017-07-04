%{
#define _POSIX_C_SOURCE 1
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define MAX_SYMB 2048

#define GLOBAL 1
#define LOCALE 2
#define ARGS 3

#define CHAR 1
#define INT 2

#define ENTER 10

static FILE* file;

typedef struct {
	int identifiant;
	int scope;
	int type;
	int address;
	int add_info;
}Symbole;

typedef struct {
	Symbole *symb[MAX_SYMB];
	int base;
	int sommet;
}Table;

Symbole * Allocation_Symbole(int identifiant, int scope, int type, int address, int add_info);

Symbole * Search_Symbole(Table * table, int identifiant, int scope);

void Insert_Symbole(Table * table, int identifiant, int type, int scope, int address, int add_info);

Table * Allocation_Table();

int yyerror(char *);
int yylex();
	Table * table;
	FILE * yyin;
	int yyval;
	int jump_label = 0;
	int alloc=0;
	void inst(const char *);
	void instarg(const char *, int);
	void comment(const char *);
%}

%union {
	int entier;
	char caractere;
	char * instr;
	char id[256];
	int type;
	int scope;
}

%token IF ELSE PRINT READ READCH WHILE IDENT CONST TYPE MAIN VOID LPAR RPAR LACC RACC LSQB RSQB
%token <entier> NUM 
%token <caractere> CARACTERE 
%token COMP BOPE PV VRG EGAL ADDSUB DIVSTAR NEGATION RETURN

%type <id>IDENT
%type <instr> ADDSUB DIVSTAR COMP BOPE


%left epsilon 
%right EGAL
%left BOPE
%left COMP
%left ADDSUB
%left DIVSTAR
%right NEGATION
%left ELSE WHILE
%right RETURN

%% 

Prog: 	
	{table = Allocation_Table();}
	DeclConst DeclVarPuisFonct DeclMain {$<entier>3=$<entier>1;$<scope>3=$<scope>1;$<type>3=$<type>1;}
;
DeclConst: 	
	DeclConst CONST ListConst PV {$<entier>$=$<entier>3;$<scope>$=$<scope>3;$<type>$=$<type>3;}
	| 
;
ListConst: 	
	ListConst VRG IDENT EGAL Litteral
		{$<entier>3=alloc;	alloc+=1; 	instarg("ALLOC",1); 	instarg("SET", alloc-1); 
		inst("SWAP"); 	instarg("SET",$<entier>5); 	inst("SAVE");
		Insert_Symbole(table, $<entier>3, INT, GLOBAL, alloc-1, 0);
		$<entier>$=$<entier>3;$<scope>$=GLOBAL;$<type>$=INT;
		}
	| IDENT EGAL Litteral 
		{$<entier>1=alloc;	alloc+=1; 	instarg("ALLOC",1); 	instarg("SET", alloc-1);
		inst("SWAP"); 	instarg("SET",$<entier>3); 	inst("SAVE");
		Insert_Symbole(table, $<entier>3, INT, GLOBAL, alloc-1, 0);
		$<entier>$=$<entier>1;$<scope>3=GLOBAL;$<type>$=INT;
		}
;
Litteral:	 
	NombreSigne
	| CARACTERE
;
NombreSigne:	 
	NUM
	| ADDSUB NUM
;
DeclVarPuisFonct:	
	TYPE ListVar PV DeclVarPuisFonct
	| DeclFonct
	| 
;
ListVar:
	ListVar VRG Ident
	| Ident
;
Ident:	
	IDENT Tab
;
Tab: 	
	Tab LSQB ENTIER RSQB
	| 
;
DeclMain:	
	EnTeteMain Corps {$<entier>2=$<entier>$;$<type>2=$<type>$;$<scope>2=$<scope>$;}
;
EnTeteMain: 	
	MAIN LPAR RPAR
;
DeclFonct: 	
	DeclFonct DeclUneFonct
	| DeclUneFonct
;
DeclUneFonct:	
	EnTeteFonct Corps
;
EnTeteFonct: 	
	TYPE IDENT LPAR Parametres RPAR
	| VOID IDENT LPAR Parametres RPAR
;
Parametres:	
	VOID
	| ListTypVar
;
ListTypVar: 	
	ListTypVar VRG TYPE IDENT
	| TYPE IDENT
;
Corps: 	
	LACC DeclConst DeclVar SuiteInstr RACC {$<entier>4=$<entier>$;$<type>4=$<type>$;$<scope>4=$<scope>$;}
;
DeclVar: 	
	DeclVar TYPE ListVar PV
	| 
;
SuiteInstr: 	
	SuiteInstr Instr {$<entier>2=$<entier>$;$<type>2=$<type>$;$<scope>2=$<scope>$;}
	|
;
InstrComp: 	
	LACC SuiteInstr RACC
;
Instr: 	LValue EGAL Exp PV
		{ alloc+=1; 	$<entier>1=alloc-1; 	instarg("ALLOC",1); 	
		instarg("SET", alloc-1); 	inst("SWAP");	inst("POP");	
		inst("SAVE"); $<type>1=$<type>$;$<scope>1=$<scope>$;
		}
	| IF LPAR Exp RPAR JumpIf Instr %prec epsilon
		{instarg("LABEL", $<entier>5);}
	| IF LPAR Exp RPAR JumpIf Instr ELSE JumpElse 
		{instarg("LABEL", $<entier>5);}
			Instr
				{instarg("LABEL", $<entier>8);}
	| WHILE 
		{instarg("LABEL", $<entier>$=jump_label++);}
			LPAR Exp RPAR JumpIf Instr
				{instarg("JUMP", $<entier>2);instarg("LABEL",$<entier>6);}
	| RETURN Exp PV
	| RETURN PV
	| IDENT LPAR Arguments RPAR PV
	| READ LPAR IDENT RPAR PV
		{ alloc+=1; 	$<entier>3=alloc-1; 	instarg("ALLOC",1); 	
		instarg("SET", alloc-1); 	inst("SWAP");	inst("READ");	
		inst("SAVE");	$<type>3=$<type>$;$<scope>3=$<scope>$;
		Insert_Symbole(table, $<entier>3, INT, $<type>$, alloc-1, 0);
		}
	| READCH LPAR IDENT RPAR PV	
		{ alloc+=1; 	$<entier>3=alloc-1; 	instarg("ALLOC",1); 	
		instarg("SET", alloc-1); 	inst("SWAP");	inst("READCH");	
		inst("SAVE");	$<type>3=$<type>$;$<scope>3=$<scope>$;
		Insert_Symbole(table, $<entier>3, CHAR, $<type>$, alloc-1, 0);
		}
	| PRINT LPAR Exp RPAR PV 
		{	if($<type>3==CHAR)
				{inst("POP"),	inst("WRITECH"); instarg("SET",ENTER); inst("WRITECH");}
			else {inst("POP"),	inst("WRITE");}
		}
	| PV
	| InstrComp
;
Arguments: 	
	ListExp
	| 
;
LValue: 	
	IDENT TabExp 
	
;
TabExp: 	
	TabExp LSQB Exp RSQB
	| 
;

ListExp: 	
	ListExp VRG Exp
	| Exp
;
Exp: 
	Exp ADDSUB Exp 	
		{inst("POP");	inst("SWAP");	inst("POP");	inst($2);	
		inst("PUSH");}	
	| Exp DIVSTAR Exp 	
		{inst("POP");	inst("SWAP");	inst("POP");	inst($2);	
		inst("PUSH");}
	| Exp COMP Exp
		{inst("POP");	inst("SWAP");	inst("POP");	inst($2);	
		inst("PUSH");}		
	| ADDSUB Exp
		{inst("POP");	inst("SWAP");	instarg("SET", 0);	inst($1);	
		inst("PUSH");}
	| Exp BOPE Exp
		{inst("POP");	inst("SWAP");	inst("POP");	inst($2);	
		inst("SWAP");	instarg("SET",0);	inst("NOTEQ");	inst("PUSH");}	
	| NEGATION Exp 
		{instarg("SET",0);	inst("SWAP");	inst("POP");	inst("EQUAL");	
		inst("PUSH");}
	| LPAR Exp RPAR
	| LValue 
		{	
		if(Search_Symbole(table,$<entier>$,GLOBAL)!=NULL)
			{instarg("SET",(Search_Symbole(table,$<entier>$,$<scope>$))->address);	
			inst("LOAD");
			$<type>$=(Search_Symbole(table,$<entier>$,$<scope>$))->type;}	
		inst("PUSH");	
		}
	| NUM 
		{instarg("SET",$1);	inst("PUSH");$<type>$=INT;}
	| CARACTERE
		{instarg("SET",$1);	inst("PUSH");$<type>$=CHAR;}
	| IDENT LPAR Arguments RPAR
;
JumpIf:
	{inst("POP"); instarg("JUMPF", $<entier>$=jump_label++);}
;
JumpElse: 
	{instarg("JUMP", $<entier>$=jump_label++);}
;
ENTIER:
 	NUM
	|
;
%%

Table * Allocation_Table(){
	Table * table = NULL;int i;
	if (NULL == (table = (Table *) malloc ( sizeof (Table) )))
		{perror("Allocation_Table: Table is NULL");
		exit(EXIT_FAILURE);}
	table->base = 0;
	table->sommet = 0;
	for(i=0;i<MAX_SYMB;i++)
		table->symb[i]=(Symbole *) malloc ( sizeof (Symbole) );
	return table;
}
	
Symbole * Allocation_Symbole(int identifiant, int scope, int type, int address, int add_info){
	Symbole * symb = NULL;
	if ( NULL == (symb = (Symbole *) malloc ( sizeof (Symbole) )))
		{perror("Allocation_Symbole : Symbole is NULL");
		exit(EXIT_FAILURE);}
	symb->identifiant = identifiant;	
	symb->scope = scope;
	symb->type = type;
	symb->address = address;
	symb->add_info = add_info;

	return symb;
}

Symbole * Search_Symbole(Table * table, int ident, int scope){
	int i = 0, fin = 0;	
	if(table == NULL)
		return NULL;
	fin = table->sommet;
	for( i = 0; i<fin; i++){	
		if((table->symb[i]->scope == scope) && /* (0==strcmp(table->symb[i]->identifiant , ident)*/(ident==table->symb[i]->identifiant))
			return table->symb[i];
		}	
	return NULL;
}

void Insert_Symbole(Table * table, int identifiant, int type, int scope, int address, int add_info){
	if(NULL != Search_Symbole(table, identifiant, scope)){
		fprintf(stderr,"variable %d already exists\n",identifiant);
		return;
	}
	if(MAX_SYMB <= table->sommet){
		fprintf(stderr,"table of symbol is full\n");
		exit(EXIT_FAILURE);
	}
	table->symb[table->sommet] = Allocation_Symbole(identifiant, scope, type, address, add_info);	

	table->sommet += 1;
}

int yyerror(char * s){
	fprintf(stderr, "%s\n",s);
	return 0;
}

void endProgram(){fprintf(file,"HALT\n"); 
					if(file != stdout)
						fclose(file);
				}

void inst(const char * s){fprintf(file,"%s\n",s);}

void instarg(const char * s, int n){fprintf(file,"%s\t%d\n",s,n);}

void comment(const char * s){fprintf(file,"#%s\n",s);}

int main(int argc, char ** argv){
	char * str; file = stdout;
	if(argc == 2)
		yyin = fopen(argv[1],"r");
	else 
		if (argc == 1)
			yyin = stdin;
		else 	
			{
			if(argc == 3 && strcmp(argv[2],"-o")==0)
				{
				yyin = fopen(argv[1],"r");
				str=strtok(argv[1],".");
				str=strcat(argv[1],".vm");
				file = fopen(str,"w");
				}	
			else {
				fprintf(stderr, "usage: %s [src] [-o]\n",argv[0]);
				return 1;
				}
			}	
	yyparse();
	endProgram();
	return 0;
}


