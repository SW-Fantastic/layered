
#include <clang/AST/ASTConsumer.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/FrontendAction.h>
#include <clang/Tooling/CommonOptionsParser.h>
#include <clang/Tooling/Tooling.h>
#include <clang/ASTMatchers/ASTMatchFinder.h>
#include <clang/ASTMatchers/ASTMatchers.h>
#include <clang/AST/Type.h>

#include "../include/AnalysiserVisitors.h"
#include "../include/AnalysiserDeclare.h"
#include "../include/AnalysiserContext.h"
#include "../include/HeaderAnalysiser.h"
#include "../include/AnalysiserDiagnostic.h"

#include "../include/jni/jni.h"

using namespace clang;
using namespace clang::ast_matchers;
using namespace clang::tooling;
using namespace llvm;


void parseSourceFile(const char* workingDir, int paramCount, const char** params, int pathCount,const char** paths) {

	std::string result = AnalysiserHelper::parseHeader(workingDir, paramCount, params, pathCount, paths);
	std::cout << "result : " << result << std::endl;

}


int main(int argc, char** argv) {

	const std::string pwd = ".";
	const char* params[] = { "-Xclang"};
	const char* args[] = {
		"D:\\JavaProjects\\layered\\assets\\pdfium-v152.0.7947.0\\include\\fpdfview.h"
	};

	parseSourceFile(pwd.c_str(), 0, params, 1, args);

	return 0;
}