#ifndef __VISITORS_H_
#define __VISITORS_H_

#include "clang/ASTMatchers/ASTMatchFinder.h"
#include "clang/ASTMatchers/ASTMatchers.h"

#include "AnalysiserContext.h"

using ClangType = std::variant<clang::QualType, const clang::FunctionDecl*>;

class ParserHelper {

public:

	static std::string getScope(clang::QualType type);
	static std::string getScope(const clang::DeclContext* ctx);
	static std::string getTypeName(clang::QualType type);
	static clang::QualType getTypeNoRefPtr(clang::QualType type);
	static bool isStructType(clang::QualType type);
	static bool isClassType(clang::QualType type);

};

class MetaParser {

public:
	
	MetaParser(AnalysiserContext* ctx) {
		this->context = ctx;
		this->parent = nullptr;
	}

	MetaParser(AnalysiserContext* ctx, MetaParser* parent) {
		this->context = ctx;
		this->parent = parent;
	}

	virtual std::string parse(ClangType node) = 0;

	std::string touch(ClangType type) {
		if (this->parent) {
			return this->parent->parse(type);
		}
		return "";
	}

	virtual ~MetaParser() {

	}

protected:

	AnalysiserContext* context;
	MetaParser* parent;

};


class StructParser : public MetaParser {

public: 

	StructParser(AnalysiserContext * context) : MetaParser(context) {

	}

	StructParser(AnalysiserContext* context, MetaParser* parent) : MetaParser(context, parent) {

	}

	std::string parse(ClangType decl) override;

};

class PointerParser : public MetaParser {

public:

	PointerParser(AnalysiserContext* context) : MetaParser(context) {
	}

	PointerParser(AnalysiserContext* context, MetaParser* parent) : MetaParser(context, parent) {
	}

	std::string parse(ClangType decl) override;

};


class FunctionProtoParser : public MetaParser {

public:

	FunctionProtoParser(AnalysiserContext* context) : MetaParser(context) {

	}

	FunctionProtoParser(AnalysiserContext* context, MetaParser* parent) : MetaParser(context, parent) {

	}

	std::string parse(ClangType decl) override;

};

class FunctionParser : public MetaParser {

public:

	FunctionParser(AnalysiserContext* context) : MetaParser(context) {

	}

	FunctionParser(AnalysiserContext* context, MetaParser* parent) :MetaParser(context, parent) {

	}

	std::string parse(ClangType decl) override;

};

class IntegratedParser : public MetaParser {

public:

	IntegratedParser(AnalysiserContext* ctx) : MetaParser(ctx) {
		
		this->structParser = new StructParser(ctx, this);
		this->pointerParser = new PointerParser(ctx, this);
		this->functionParser = new FunctionParser(ctx, this);
		this->functionProtoParser = new FunctionProtoParser(ctx, this);
	}


	std::string parse(ClangType type);

	~IntegratedParser() {
		delete this->structParser;
		delete this->pointerParser;
		delete this->functionParser;
		delete this->functionProtoParser;
	}

private:
	
	StructParser* structParser;
	PointerParser* pointerParser;
	FunctionParser* functionParser;
	FunctionProtoParser* functionProtoParser;

};

class AnalysiserCallback : public clang::ast_matchers::MatchFinder::MatchCallback {

public:

	AnalysiserCallback(AnalysiserContext* ctx) {
		this->context = ctx;
		this->parser = new IntegratedParser(ctx);
	}

	void run(const clang::ast_matchers::MatchFinder::MatchResult& result) override;

	~AnalysiserCallback() {
		delete this->parser;
	}

protected:

	IntegratedParser* parser;
	AnalysiserContext* context;

};

#endif