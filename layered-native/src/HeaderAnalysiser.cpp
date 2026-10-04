#include "../include/HeaderAnalysiser.h"
#include "../include/AnalysiserContext.h"
#include "../include/AnalysiserVisitors.h"
#include "../include/AnalysiserDiagnostic.h"

#include <clang/ASTMatchers/ASTMatchFinder.h>
#include <clang/ASTMatchers/ASTMatchers.h>
#include <clang/Tooling/CommonOptionsParser.h>
#include <clang/Tooling/Tooling.h>



using namespace clang;
using namespace clang::ast_matchers;

ParseConsumer::ParseConsumer(AnalysiserContext* ctx) : context(ctx) {

}

void ParseConsumer::HandleTranslationUnit(clang::ASTContext& Ctx) {

	clang::PrintingPolicy policy = Ctx.getPrintingPolicy();
	policy.SuppressInlineNamespace = true;
	policy.FullyQualifiedName = true;
	Ctx.setPrintingPolicy(policy);

	MatchFinder* finder = new  MatchFinder();

	auto globalFuncMatcher = functionDecl(
		hasDeclContext(anyOf(
			translationUnitDecl(),
			namespaceDecl()
		)), unless(anyOf(
			cxxMethodDecl(),
			isExpansionInSystemHeader()
		))
	).bind("globalFunc");

	auto structMatcher = recordDecl(
		isStruct(),
		hasDeclContext(anyOf(
			translationUnitDecl(),
			namespaceDecl()
		)), unless(
			isExpansionInSystemHeader()
		)
	).bind("structDecl");

	AnalysiserCallback analasiserCallback(this->context);
	finder->addMatcher(structMatcher, &analasiserCallback);
	finder->addMatcher(globalFuncMatcher, &analasiserCallback);
	finder->matchAST(Ctx);

	delete finder;

}


AnalysiserActionFactory::AnalysiserActionFactory(AnalysiserContext* ctx) :context(ctx) {

}

std::unique_ptr<clang::FrontendAction> AnalysiserActionFactory::create() {

	return std::make_unique<ParserAction>(this->context);

}


ParserAction::ParserAction(AnalysiserContext* ctx) : context(ctx) {

}

std::unique_ptr<clang::ASTConsumer> ParserAction::CreateASTConsumer(clang::CompilerInstance& CI, llvm::StringRef file) {

	return std::make_unique<ParseConsumer>(this->context);

}

std::string AnalysiserHelper::parseHeader(const char* workingDir, int paramCount, const char** params, int pathCount, const char** paths) {
	std::vector<std::string> theParams;
	for (int index = 0; index < paramCount; index++) {
		std::string str(params[index]);
		theParams.push_back(str);
	}


	std::string result = "{}";
	std::vector<std::string> theSources;
	for (int index = 0; index < pathCount; index++) {
		std::string str(paths[index]);
		theSources.push_back(str);
	}

	{
		AnalysiserContext ctx;
		AnalysisDiagnosticConsumer diagnosticHandler(&ctx);
		AnalysiserActionFactory factory(&ctx);
		clang::tooling::FixedCompilationDatabase complationDB(workingDir, theParams);
		clang::tooling::ClangTool tool(complationDB, theSources);
		tool.setDiagnosticConsumer(&diagnosticHandler);

		tool.run(&factory);
		result = ctx.writeAsJson();
		ctx.clear();

	}

	llvm::llvm_shutdown();
	return result;
}