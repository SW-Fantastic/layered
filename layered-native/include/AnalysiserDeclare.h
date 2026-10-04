#ifndef __VISITOR_DECL_H_
#define __VISITOR_DECL_H_

#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Type.h>
#include <clang/Frontend/FrontendAction.h>
#include <llvm/ADT/StringRef.h>
#include <llvm/Support/JSON.h>
#include <llvm/Support/Base64.h>

#include <string>
#include <variant>
#include <unordered_map>
#include <stack>

const inline char* SPLITER = "::";


enum DescriptorType {

	UNKNOWN   = 0,
	POINTER   = 1,
	FUNCTION  = 2,
	RECORD    = 3,
	BUILT_IN  = 4

};


class BaseTypeDescriptor {

public:

	virtual DescriptorType getType();
	virtual std::string getTypeStr();
	virtual std::string getId();
	virtual std::string getName();
	virtual std::string getNamespace();
	virtual llvm::json::Object getJson();

protected:

	std::string nsName;
	std::string name;

	bool isConst = false;
	bool isVolatile = false;


};


template<typename C> 
class TypeDescriptor: public BaseTypeDescriptor {
	
public:

	TypeDescriptor(std::string name, std::string namespacePath) {

		this->name = name;
		this->nsName = namespacePath;

		this->isConst = false;
		this->isVolatile = false;

	}

	virtual C* copy() = 0;

	C* copyConst() {
		C* target = copy();
		target->isConst = true;
		return target;
	}

	C* copyVolatile() {
		C* target = copy();
		target->isVolatile = true;
		return target;
	}

	C* copyConstVolatile() {
		C* target = copy();
		target->isVolatile = true;
		target->isConst = true;
		return target;
	}


};

class BuiltInDescriptor: public TypeDescriptor<BuiltInDescriptor> {

public:

	BuiltInDescriptor(std::string name, std::string symbol);

	std::string getId() override;
	BuiltInDescriptor* copy() override;
	DescriptorType getType() override;
	std::string getTypeStr() override;
	virtual llvm::json::Object getJson() override;
	std::string getSymbol();


private:

	std::string symbol;

};

class FunctionDescriptor : public TypeDescriptor<FunctionDescriptor> {

public:


	FunctionDescriptor(

		std::string name,
		std::string namespacePath,
		std::string returnType,
		int paramCount,
		std::vector<std::string> params,
		bool callback

	);

	std::string getId() override;
	std::string generateParamSymbol(std::string paramStr);
	FunctionDescriptor* copy() override;
	DescriptorType getType() override;
	std::string getTypeStr() override;
	llvm::json::Object getJson() override;

protected:

	std::string returnTypeId;
	int paramCount = 0;
	std::vector<std::string> paramTypes;
	bool callback = false;

};


class RecordTypeDescriptor : public TypeDescriptor<RecordTypeDescriptor>{

public:

	RecordTypeDescriptor(std::string name, std::string namespacePath);

	DescriptorType getType() override;
	std::string getTypeStr() override;
	RecordTypeDescriptor* copy() override;
	void updateField(std::string fieldName, std::string typeId);
	llvm::json::Object getJson();

protected:

	std::unordered_map<std::string, std::string> fields;
	
	std::unordered_map<std::string, std::string> constructors;
	std::unordered_map<std::string, std::string> publicMethods;
	std::unordered_map<std::string, std::string> overrideableMethods;

};

class PointerTypeDescriptor : public TypeDescriptor<PointerTypeDescriptor>{

public:

	PointerTypeDescriptor(std::string targetTypeId, std::string targetTypeName);

	DescriptorType getType() override;
	std::string getTypeStr() override;
	std::string getName() override;
	PointerTypeDescriptor* copy() override;
	std::string getId() override;
	llvm::json::Object getJson() override;
 
protected:

	std::string targetTypeId;

};



#endif