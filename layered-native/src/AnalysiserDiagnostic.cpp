#include "../include/AnalysiserDiagnostic.h"

AnalysisDiagnosticConsumer::AnalysisDiagnosticConsumer(AnalysiserContext* ctx) {
	this->context = ctx;
}

void AnalysisDiagnosticConsumer::HandleDiagnostic(
	clang::DiagnosticsEngine::Level diagLevel,
	const clang::Diagnostic& info
) {

	std::string level = "";
	if (diagLevel == clang::DiagnosticsEngine::Error) {
		level = "Error";
	} else if (diagLevel == clang::DiagnosticsEngine::Warning) {
		level = "Warning";
	} else if (diagLevel == clang::DiagnosticsEngine::Fatal) {
		level = "Fatal";
	} else {
		level = "Info";
	}

	DiagnosticMsg* diagnosticMsg = new DiagnosticMsg(level);

	llvm::SmallString<256> messageStr;
	info.FormatDiagnostic(messageStr);
	std::string message = messageStr.c_str();

	diagnosticMsg->setMessage(message);

	if (info.hasSourceManager()) {

		clang::SourceManager& sourceMgr = info.getSourceManager();
		clang::SourceLocation loc = info.getLocation();
		if (loc.isValid()) {
			
			clang::SourceLocation spellingLoc = sourceMgr.getSpellingLoc(loc);
			diagnosticMsg->setLocation(
				sourceMgr.getFilename(spellingLoc).str(),
				sourceMgr.getSpellingLineNumber(spellingLoc),
				sourceMgr.getSpellingColumnNumber(spellingLoc)
			);

		}
	}

	this->context->appendLog(diagnosticMsg);

}