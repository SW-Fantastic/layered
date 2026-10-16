
#include "../include/AnalysiserContext.h"

AnalysiserContext::AnalysiserContext() {
	
	this->initBuiltInTypes();

}

void AnalysiserContext::initBuiltInTypes() {

	this->pushBuiltType(new BuiltInDescriptor("void","V"));
	this->pushBuiltType(new BuiltInDescriptor("int","I"));
	this->pushBuiltType(new BuiltInDescriptor("unsigned int","Ui"));
	this->pushBuiltType(new BuiltInDescriptor("short","S"));
	this->pushBuiltType(new BuiltInDescriptor("unsigned short","Us"));
	this->pushBuiltType(new BuiltInDescriptor("long","L"));
	this->pushBuiltType(new BuiltInDescriptor("unsigned long", "Ul"));
	this->pushBuiltType(new BuiltInDescriptor("long long", "Ll"));
	this->pushBuiltType(new BuiltInDescriptor("unsigned long long", "Ull"));
	this->pushBuiltType(new BuiltInDescriptor("float","F"));
	this->pushBuiltType(new BuiltInDescriptor("double","D"));
	this->pushBuiltType(new BuiltInDescriptor("char", "C"));
	this->pushBuiltType(new BuiltInDescriptor("unsigned char","B"));

	this->pushBuiltType(new BuiltInDescriptor("size_t","Sz"));

}

BaseTypeDescriptor* AnalysiserContext::getByUnModifierId(std::string name, int isConst, int isVoliate) {

	std::string id = name;
	if (isConst && isVoliate) {
		id = "[CV]" + name;
	}
	else if (isConst) {
		id = "[C]" + name;
	}
	else if (isVoliate) {
		id = "[V]" + name;
	}

	return this->getById(id);

}

BaseTypeDescriptor* AnalysiserContext::getById(std::string id) {

	if (this->builtInDesc.count(id)) {
		return this->builtInDesc.at(id);
	}
	else if (this->functionDesc.count(id)) {
		return this->functionDesc.at(id);
	}
	else if (this->structDesc.count(id)) {
		return this->structDesc.at(id);
	}
	else if (this->classDesc.count(id)) {
		return this->classDesc.at(id);
	}
	else if (this->pointerDesc.count(id)) {
		return this->pointerDesc.at(id);
	}

	return nullptr;

}

PointerTypeDescriptor* AnalysiserContext::pushPointerType(PointerTypeDescriptor* rec) {

	if (!rec) {
		return nullptr;
	}

	std::string key = rec->getId();
	if (this->pointerDesc.count(key)) {
		PointerTypeDescriptor* target = this->pointerDesc.at(key);
		delete rec;
		return target;
	}
	this->pointerDesc.insert_or_assign(key, rec);
	return rec;

}

RecordTypeDescriptor* AnalysiserContext::pushStructType(RecordTypeDescriptor* rec) {

	if (!rec) {
		return nullptr;
	}

	std::string key = rec->getId();
	if (this->structDesc.count(key)) {
		RecordTypeDescriptor* target = this->structDesc.at(key);
		delete rec;
		return target;
	}
	this->structDesc.insert_or_assign(key, rec);
	return rec;
}

BuiltInDescriptor* AnalysiserContext::pushBuiltType(BuiltInDescriptor* desc) {

	if (!desc) {
		return nullptr;
	}

	BuiltInDescriptor* cDesc = desc->copyConst();
	BuiltInDescriptor* vDesc = desc->copyVolatile();
	BuiltInDescriptor* cvDesc = desc->copyConstVolatile();

	if (builtInDesc.count(cDesc->getId())) {
		delete cDesc;
	} else {
		builtInDesc.insert_or_assign(cDesc->getId(), cDesc);
	}

	if (builtInDesc.count(vDesc->getId())) {
		delete vDesc;
	} else {
		builtInDesc.insert_or_assign(vDesc->getId(), vDesc);
	}

	if (builtInDesc.count(cvDesc->getId())) {
		delete cvDesc;
	} else {
		builtInDesc.insert_or_assign(cvDesc->getId(), cvDesc);
	}

	BuiltInDescriptor* result = nullptr;
	if (builtInDesc.count(desc->getId())) {
		result = builtInDesc.at(desc->getId());
		delete desc;
	} else {
		builtInDesc.insert_or_assign(desc->getId(), desc);
		result = desc;
	}

	builtInSymbosMap.insert_or_assign(result->getName(), result->getSymbol());
	return result;

}

std::string AnalysiserContext::writeJson(BaseTypeDescriptor* desc) {

	std::string buf;
	llvm::raw_string_ostream osbuf(buf);

	llvm::json::Object obj = desc->getJson();
	osbuf << llvm::formatv("{0:4}", llvm::json::Value(std::move(obj)));

	return buf;
}

int AnalysiserContext::existById(std::string id) {

	return getById(id) != nullptr;

}

FunctionDescriptor* AnalysiserContext::put(FunctionDescriptor* funcType) {

	if (functionDesc.count(funcType->getId())) {
		FunctionDescriptor* result = this->functionDesc.at(funcType->getId());
		delete funcType;
		return result;
	}

	this->functionDesc.insert_or_assign(funcType->getId(), funcType);
	return funcType;

}

PointerTypeDescriptor* AnalysiserContext::put(PointerTypeDescriptor* ptrType) {

	if (!ptrType) {
		return nullptr;
	}

	ptrType = this->pushPointerType(ptrType);
	return ptrType;

}

RecordTypeDescriptor* AnalysiserContext::put(RecordTypeDescriptor* recType) {

	if (!recType) {
		return nullptr;
	}

	recType = this->pushStructType(recType);
	if (recType->hasAnyModifier()) {
		this->pushStructType(recType->copyRaw());
	}
	return recType;

}

std::string AnalysiserContext::getBuiltInSymbol(std::string builtInName) {
	if (this->builtInSymbosMap.count(builtInName)) {
		return this->builtInSymbosMap.at(builtInName);
	}
	return "";
}

void AnalysiserContext::appendLog(DiagnosticMsg* msg) {
	this->logs.push_back(msg);
}

void AnalysiserContext::clear() {

	for (auto& [key, desc] : this->structDesc) {
		if (desc) {
			delete desc;
		}
	}

	this->structDesc.clear();

	for (auto& [key, desc] : this->classDesc) {
		if (desc) {
			delete desc;
		}
	}
	this->classDesc.clear();

	for (auto& [key, desc] : this->functionDesc) {
		if (desc) {
			delete desc;
		}
	}
	this->functionDesc.clear();

	for (auto& [key, desc] : this->pointerDesc) {
		if (desc) {
			delete desc;
		}
	}
	this->pointerDesc.clear();

	for (auto& [key, desc] : this->builtInDesc) {
		if (desc) {
			delete desc;
		}
	}

	for (auto& value : this->logs) {
		delete value;
	}
	this->logs.clear();

	this->builtInDesc.clear();
	this->builtInSymbosMap.clear();

}

std::vector<RecordTypeDescriptor*> AnalysiserContext::getStructs() {
	
	std::vector<RecordTypeDescriptor*> values;
	values.reserve(this->structDesc.size());
	
	for (const auto& pair : this->structDesc) {
		values.push_back(pair.second);
	}
	return values;

}

std::string AnalysiserContext::writeAsJson() {

	std::string buf;
	llvm::raw_string_ostream out(buf);

	llvm::json::Object builtInTypes;
	for (auto& [key, ptr] : this->builtInDesc) {
		builtInTypes[key] = ptr->getJson();
	}

	llvm::json::Object structTypes;
	for (auto& [key, ptr] : this->structDesc) {
		structTypes[key] = ptr->getJson();
	}

	llvm::json::Object functionTypes;
	for (auto& [key, ptr] : this->functionDesc) {
		functionTypes[key] = ptr->getJson();
	}

	llvm::json::Object pointerTypes;
	for (auto& [key, ptr] : this->pointerDesc) {
		pointerTypes[key] = ptr->getJson();
	}

	llvm::json::Array logs;
	for (auto log : this->logs) {
		logs.push_back(log->asJson());
	}

	llvm::json::Object result {
		{ "builtIn",    std::move(builtInTypes) },
		{ "structType", std::move(structTypes)  },
		{ "functions",  std::move(functionTypes)},
		{ "pointers",   std::move(pointerTypes) },
		{ "exceptions", std::move(logs)}
	};

	out << llvm::formatv("{0:4}", llvm::json::Value(std::move(result)));
	return buf;

}