
#include "../include/AnalysiserVisitors.h"
#include "../include/AnalysiserDeclare.h"
#include <clang/AST/ASTContext.h>
#include <string>


void AnalysiserCallback::run(const clang::ast_matchers::MatchFinder::MatchResult& result) {


	const clang::FunctionDecl* funcDecl = result.Nodes.getNodeAs<clang::FunctionDecl>("globalFunc");
	if (funcDecl) {
		// do parse a function
		this->parser->parse(funcDecl);
		return;
	}

	const clang::RecordDecl* recDecl = result.Nodes.getNodeAs<clang::RecordDecl>("structDecl");
	if (recDecl) {
		clang::ASTContext* context = result.Context;
		clang::QualType type = context->getTypeDeclType(clang::cast<clang::TypeDecl>(recDecl));
		this->parser->parse(type);
		return;
	}

}

std::string ParserHelper::getScope(const clang::DeclContext* typeDeclCtx) {

	std::string typeNamespace = "";
	const clang::DeclContext* current = typeDeclCtx;
	while (current && !current->isTranslationUnit()) {

		const clang::CXXRecordDecl* cxxRecordDecl = llvm::dyn_cast<clang::CXXRecordDecl>(current);
		if (cxxRecordDecl) {
			const clang::ClassTemplateSpecializationDecl* templateSpecDecl = llvm::dyn_cast<clang::ClassTemplateSpecializationDecl>(cxxRecordDecl);
			if (templateSpecDecl) {

			}
			else {
				typeNamespace = cxxRecordDecl->getNameAsString() + SPLITER + typeNamespace;
			}
		}

		const clang::NamespaceDecl* namespaceDecl = llvm::dyn_cast<clang::NamespaceDecl>(current);
		if (namespaceDecl) {
			if (!namespaceDecl->isAnonymousNamespace()) {
				typeNamespace = namespaceDecl->getNameAsString() + SPLITER + typeNamespace;
			}
		}
		current = current->getParent();
	}

	if (typeNamespace.ends_with(SPLITER)) {
		typeNamespace = typeNamespace.substr(0, typeNamespace.length() - 2);
	}

	return typeNamespace;

}

clang::QualType ParserHelper::getTypeNoRefPtr(clang::QualType type) {

	while (type->isPointerType() || type->isReferenceType()) {
		if (type->isPointerType()) {
			type = type->getPointeeType();
		}
		else if (type->isReferenceType()) {
			type = type.getNonReferenceType();
		}
	}

	return type;

}

bool ParserHelper::isStructType(clang::QualType type) {

	type = type.getCanonicalType();
	clang::TagDecl* typeDecl = type->getAsTagDecl();
	return typeDecl->getTagKind() == clang::TagTypeKind::Struct;

}

bool ParserHelper::isClassType(clang::QualType type) {

	type = type.getCanonicalType();
	clang::TagDecl* typeDecl = type->getAsTagDecl();
	return typeDecl->getTagKind() == clang::TagTypeKind::Class;

}

bool ParserHelper::haveDefaultConstructor(clang::RecordDecl* decl) {
	
	if (!decl->isCompleteDefinition()) {
		// incomplete definition，do not support new operator
		return false;
	}

	clang::CXXRecordDecl* cxxDecl = llvm::dyn_cast<clang::CXXRecordDecl>(decl);
	if (!cxxDecl || cxxDecl->isAggregate()) {
		// C struct，support new operator
		return true;
	}

	if (cxxDecl->isAbstract()) {
		// abstruct class, does not support new operator
		return false;
	}

	for (const auto ctor : cxxDecl->ctors()) {
		if (ctor->isDefaultConstructor()) {
			// the class has default ctor, check does it accessable.
			return (!ctor->isDeleted() && ctor->getAccess() == clang::AS_public);
		}
	}

	return false;

}

bool ParserHelper::isRecordMovable(clang::RecordDecl* decl) {
	
	if (!decl->isCompleteDefinition()) {
		return false;
	}
	clang::CXXRecordDecl* cxxDecl = llvm::dyn_cast<clang::CXXRecordDecl>(decl);
	if (!cxxDecl) {
		// c struct , moveable
		return true;
	}

	if (cxxDecl->isAggregate()) {
		// c struct include some c++ fields
		return true;
	}

	for (const auto ctor : cxxDecl->ctors()) {
		if (ctor->isMoveConstructor() || ctor->isCopyConstructor()) {
			if (!ctor->isDeleted() && ctor->getAccess() == clang::AS_public) {
				return true;
			}
		}
	}

	return true;
}

std::string ParserHelper::getScope(clang::QualType type) {


	if (type->isPointerType() || type->isReferenceType()) {
		type = ParserHelper::getTypeNoRefPtr(type);
	}

	type = type.getCanonicalType();

	clang::TagDecl* typeDecl = type->getAsTagDecl();
	if (type->isBuiltinType()) {
		return "";
	}
	
	clang::TypedefNameDecl* alias = typeDecl->getTypedefNameForAnonDecl();
	const clang::DeclContext* typeDeclCtx = nullptr;

	if (alias) {
		typeDeclCtx = alias->getDeclContext();
	} else {
		typeDeclCtx = typeDecl->getDeclContext();
	}
	return ParserHelper::getScope(typeDeclCtx);

}

std::string ParserHelper::getTypeName(clang::QualType type) {

	clang::QualType rawType = type;
	type = type.getCanonicalType();
	if (type->isPointerType() || type->isReferenceType()) {
		type = ParserHelper::getTypeNoRefPtr(type);
	}

	if (type->isBuiltinType()) {
		if (rawType.getAsString().rfind("size_t") != std::string::npos) {
			return "size_t";
		}
		type = type->getCanonicalTypeUnqualified();
		return type.getAsString();
	}
	clang::TagDecl* typeDecl = type->getAsTagDecl();
	clang::TypedefNameDecl* alias = typeDecl->getTypedefNameForAnonDecl();
	if (alias) {
		return alias->getNameAsString();
	}
	return typeDecl->getNameAsString();

}


std::string FunctionProtoParser::parse(ClangType rawType) {

	clang::QualType* typePtr = std::get_if<clang::QualType>(&rawType);
	if (!typePtr) {
		return "";
	}

	const clang::QualType qualType = *typePtr;
	const clang::FunctionProtoType* funcProtoType = qualType->getAs<clang::FunctionProtoType>();
	if (!funcProtoType) {
		return "";
	}

	std::string retTypeId = touch(funcProtoType->getReturnType());
	if (retTypeId.empty()) {
		return "";
	}
	std::string funcName = "_callback_" + std::to_string(funcProtoType->getNumParams());
	
	std::vector<std::string> params;
	for (int index = 0; index < funcProtoType->getNumParams(); index++) {
		std::string typeId = this->touch(funcProtoType->getParamType(index));
		if (typeId.empty()) {
			DiagnosticMsg* msg = new DiagnosticMsg("Warn");
			msg->setMessage("Can not parse function : " + funcName + " parameter type arg" + (char)( '0' + index ) + " not found.");
			this->context->appendLog(msg);
			return "";
		}
		params.push_back(typeId);
	}

	FunctionDescriptor* descriptor = new FunctionDescriptor(
		funcName, "", retTypeId, params.size(), params, true
	);

	descriptor = this->context->put(descriptor);

	return descriptor->getId();

}


std::string FunctionParser::parse(ClangType rawType) {
	
	const clang::FunctionDecl** typeDecl = std::get_if<const clang::FunctionDecl*>(&rawType);

	if (!typeDecl) {
		return "";
	}

	const clang::FunctionDecl* decl = *typeDecl;
	if (!decl) {
		return "";
	}

	std::string scope = ParserHelper::getScope(decl->getDeclContext());
	std::string funcName = decl->getNameAsString();
	std::vector<std::string> params;


	std::string returnTypeId = touch(decl->getReturnType());
	if (returnTypeId.empty()) {
		return "";
	}

	for (const clang::ParmVarDecl* paramDecl : decl->parameters()) {
		
		std::string typeId = this->touch(paramDecl->getType());
		if (typeId.empty()) {
			DiagnosticMsg* msg = new DiagnosticMsg("Warn");
			msg->setMessage("Can not parse function : " + funcName + " parameter type " + paramDecl->getNameAsString() + " not found.");
			this->context->appendLog(msg);
			return "";
		}
		params.push_back(typeId);

	}

	FunctionDescriptor* descriptor = new FunctionDescriptor(
		funcName, scope, returnTypeId, params.size(), params, false
	);

	descriptor = this->context->put(descriptor);

	return descriptor->getId();
}


std::string PointerParser::parse(ClangType rawType) {
	
	clang::QualType* typeDeclPtr = std::get_if<clang::QualType>(&rawType);
	
	if (!typeDeclPtr) {
		return "";
	}

	clang::QualType typeDecl = *typeDeclPtr;
	if (!typeDecl->isPointerType()) {
		return "";
	}

	std::stack<clang::QualType> types;
	clang::QualType curType = typeDecl;
	while (curType->isPointerType()) {
		types.push(curType);
		curType = curType->getPointeeType();
	}
	
	std::string targetTypeId = touch(curType);
	if (targetTypeId.empty()) {
		return "";
	}
	BaseTypeDescriptor* desc = this->context->getById(targetTypeId);
	if (desc == nullptr) {
		return "";
	}

	PointerTypeDescriptor * result = nullptr;
	while (!types.empty()) {
		clang::QualType innerType = types.top();
		if (result == nullptr) {
			result = new PointerTypeDescriptor(
				targetTypeId, desc->getName(), 
				innerType.isConstQualified(), 
				innerType.isVolatileQualified()
			);
			result = this->context->put(result);
		} else {
			PointerTypeDescriptor* next = new PointerTypeDescriptor(
				result->getId(),
				result->getName(), 
				innerType.isConstQualified(), 
				innerType.isVolatileQualified()
			);
			next = this->context->put(next);
			result = next;
		}
		types.pop();
	}
	return result->getId();

}

std::string StructParser::parse(ClangType rawType) {

	clang::QualType* typeDeclPtr = std::get_if<clang::QualType>(&rawType);

	if (!typeDeclPtr) {
		return "";
	}

	clang::QualType decl = *typeDeclPtr;
	clang::QualType qType = decl.getCanonicalType();
	const clang::RecordType* recordType = qType->getAs<clang::RecordType>();
	if (!recordType) {
		return "";
	}
	clang::RecordDecl* recordDecl = recordType->getDecl();

	std::string qName = ParserHelper::getTypeName(qType);
	std::string nsName = ParserHelper::getScope(qType);
	if (qName.empty()) {
		return "";
	}

	RecordTypeDescriptor* result = new RecordTypeDescriptor(
		qName, nsName,
		qType.isConstQualified(), qType.isVolatileQualified(),
		ParserHelper::isRecordMovable(recordDecl),
		ParserHelper::haveDefaultConstructor(recordDecl),
		recordDecl->isCompleteDefinition()
	);

	result = this->context->put(result);

	if (result->getState() == DescriptorState::PROCESSING) {
		return result->getId();
	}

	result->updateState(DescriptorState::PROCESSING);

	for (const clang::FieldDecl* fieldDecl : recordDecl->fields()) {

		std::string fieldName = fieldDecl->getNameAsString();
		clang::QualType type = fieldDecl->getType();
		std::string typeId = this->touch(type);
		if (typeId.empty()) {
			continue;
		}
		result->updateField(fieldName, typeId);

	}
	result->updateState(DescriptorState::RESOLVED);

	return result->getId();
	
}

std::string IntegratedParser::parse(ClangType type) {
	if (clang::QualType* qualType = std::get_if<clang::QualType>(&type)) {

		clang::QualType theType = *qualType;
		clang::QualType qualifiedType = theType.getCanonicalType();

		if (qualifiedType->isBuiltinType()) {

			std::string typeName = ParserHelper::getTypeName(*qualType);
			std::string typeSymbol = this->context->getBuiltInSymbol(typeName);
			if (typeSymbol.empty()) {
				return "";
			}

			BaseTypeDescriptor* desc = this->context->getByUnModifierId(
				typeSymbol,
				qualifiedType.isConstQualified(),
				qualifiedType.isVolatileQualified()
			);

			if (desc == nullptr) {
				return "";
			}
			return desc->getId();

		}
		else if (qualifiedType->isAnyPointerType()) {

			return this->pointerParser->parse(type);

		}
		else if (qualifiedType->isRecordType()) {

			if (ParserHelper::isStructType(qualifiedType)) {
				return this->structParser->parse(type);
			}

		}
		else if (qualifiedType->isFunctionProtoType()) {

			return this->functionProtoParser->parse(type);

		}

	}
	else if (const clang::FunctionDecl** declType = std::get_if<const clang::FunctionDecl*>(&type)) {

		return this->functionParser->parse(type);

	}

	return "";
}