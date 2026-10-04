#ifndef _HEADER_ANALYSISTER_
#define _HEADER_ANALYSISTER_

#include <clang/AST/Type.h>
#include <clang/Frontend/FrontendAction.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/Tooling/Tooling.h>

#include "AnalysiserContext.h"

class ParseConsumer : public clang::ASTConsumer {

public:

	ParseConsumer(AnalysiserContext* ctx);

	void HandleTranslationUnit(clang::ASTContext& Context);

private:

	AnalysiserContext* context;

};



class ParserAction : public clang::ASTFrontendAction {

public:

	explicit ParserAction(AnalysiserContext* ctx);

	std::unique_ptr<clang::ASTConsumer> CreateASTConsumer(
		clang::CompilerInstance& CI, llvm::StringRef file
	) override;

private:

	AnalysiserContext* context;

};

class AnalysiserActionFactory : public clang::tooling::FrontendActionFactory {

public:

	AnalysiserActionFactory(AnalysiserContext* ctx);

	std::unique_ptr<clang::FrontendAction> create() override;

private:

	AnalysiserContext* context;

};

class AnalysiserHelper {

public:

	static std::string parseHeader(const char* workingDir, int paramCount, const char** params, int pathCount, const char** paths);

};

#endif 