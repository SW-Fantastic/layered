#ifndef __VISITOR_CTX_H_
#define __VISITOR_CTX_H_

#include <llvm/Support/JSON.h>
#include "AnalysiserDeclare.h"
#include <clang/AST/ASTConsumer.h>
#include <string>
#include <unordered_map>
#include <iostream>

/*
 Log Object，while Analysiser find any compile exception we should generate 
 this object and push into the log vector of context
*/
class DiagnosticMsg {

public:

	DiagnosticMsg(std::string level) {
		this->level = level;
	}

	void setLocation(std::string path, int line, int col) {
		this->line = line;
		this->column = col;
		this->filePath = path;
	}

	void setMessage(std::string msg) {
		this->message = msg;
	}

	llvm::json::Object asJson() {
		return llvm::json::Object{
			{ "level", this->level },
			{ "filePath", this->filePath },
			{ "message", this->message },
			{ "line", this->line },
			{ "column", this->column}
		};
	}

private:

	std::string level;
	std::string filePath;
	std::string message;

	int line = 0;
	int column = 0;

};


class AnalysiserContext {

public:

	AnalysiserContext();

	int existById(std::string id);

	BaseTypeDescriptor* getByUnModifierId(std::string name, int isConst, int isVoliate);
	BaseTypeDescriptor* getById(std::string id);
	std::string getBuiltInSymbol(std::string builtInName);

	FunctionDescriptor* put(FunctionDescriptor* funcType);
	PointerTypeDescriptor* put(PointerTypeDescriptor* ptrType);
	RecordTypeDescriptor* put(RecordTypeDescriptor* recType);

	std::string writeAsJson();

	void appendLog(DiagnosticMsg* msg);
	void clear();


private:

	std::unordered_map<std::string, std::string> builtInSymbosMap;
	std::unordered_map<std::string, FunctionDescriptor*> functionDesc;
	std::unordered_map<std::string, BuiltInDescriptor*> builtInDesc;
	std::unordered_map<std::string, RecordTypeDescriptor*> structDesc;
	std::unordered_map<std::string, RecordTypeDescriptor*> classDesc;
	std::unordered_map<std::string, PointerTypeDescriptor*> pointerDesc;
	std::vector<DiagnosticMsg*> logs;

	void initBuiltInTypes();

	void pushPointerType(PointerTypeDescriptor* rec);
	void pushStructType(RecordTypeDescriptor* rec);
	void pushBuiltType(BuiltInDescriptor* desc);

	std::string writeJson(BaseTypeDescriptor* desc);

};

#endif
