#ifndef _ANALYSISER_DIAGNOSTIC_
#define _ANALYSISER_DIAGNOSTIC_


#include<string>
#include<clang/Basic/Diagnostic.h>
#include "AnalysiserContext.h"

class AnalysisDiagnosticConsumer : public clang::DiagnosticConsumer {

public:

	explicit AnalysisDiagnosticConsumer(AnalysiserContext* context);

	void HandleDiagnostic(clang::DiagnosticsEngine::Level DiagLevel,const clang::Diagnostic& Info) override;

private:

	AnalysiserContext* context;

};

#endif // !_ANALYSISER_DIAGNOSTIC_
