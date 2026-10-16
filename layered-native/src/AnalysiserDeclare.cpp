#include "../include/AnalysiserDeclare.h"

#include"murmur3.h"

DescriptorType BaseTypeDescriptor::getType() {
	return DescriptorType::UNKNOWN;
}

std::string BaseTypeDescriptor::getTypeStr() {
	return "UNKNOWN";
}

std::string BaseTypeDescriptor::getId() {
	std::string cv = "";
	if (this->isConst && this->isVolatile) {
		cv = "[CV]";
	}
	else if (this->isVolatile) {
		cv = "[V]";
	}
	else if (this->isConst) {
		cv = "[C]";
	}
	std::string ns = this->getNamespace();
	if (!ns.empty() && !ns.ends_with(SPLITER)) {
		ns = ns + SPLITER;
	}
	return cv + ns + name;
}

std::string BaseTypeDescriptor::getName() {
	return this->name;
}

std::string BaseTypeDescriptor::getNamespace() {
	return this->nsName;
}

llvm::json::Object BaseTypeDescriptor::getJson() {

	return llvm::json::Object{
		{ "id", this->getId() },
		{ "namespace", this->getNamespace() },
		{ "name", this->getName() },
		{ "const", this->isConst },
		{ "volatile", this->isVolatile },
		{ "type", this->getTypeStr() }
	};

}


BuiltInDescriptor::BuiltInDescriptor(std::string name, std::string symbol) :TypeDescriptor(name, "") {
	this->symbol = symbol;
}

std::string BuiltInDescriptor::getId() {
	std::string cv = "";
	if (this->isConst && this->isVolatile) {
		cv = "[CV]";
	}
	else if (this->isVolatile) {
		cv = "[V]";
	}
	else if (this->isConst) {
		cv = "[C]";
	}
	return cv + this->getSymbol();
}

BuiltInDescriptor* BuiltInDescriptor::copy() {
	return new BuiltInDescriptor(this->getName(), this->symbol);
}

DescriptorType BuiltInDescriptor::getType() {
	return DescriptorType::BUILT_IN;
}

std::string BuiltInDescriptor::getSymbol() {
	return this->symbol;
}

std::string BuiltInDescriptor::getTypeStr() {
	return "BUILT_IN";
}

llvm::json::Object BuiltInDescriptor::getJson() {

	return llvm::json::Object{
		{ "id", this->getId() },
		{ "namespace", this->getNamespace() },
		{ "name", this->getName() },
		{ "const", this->isConst },
		{ "volatile", this->isVolatile },
		{ "type", this->getTypeStr() },
		{ "symbol", this->getSymbol() }
	};

}


FunctionDescriptor::FunctionDescriptor(

	std::string name,
	std::string namespacePath,
	std::string returnType,
	int paramCount,
	std::vector<std::string> params,
	bool callback

) : TypeDescriptor(name, namespacePath) {

	this->paramCount = paramCount;
	this->paramTypes = params;
	this->returnTypeId = returnType;
	this->callback = callback;
}

std::string FunctionDescriptor::getId() {

	std::string result = "";
	for (int idx = 0; idx < this->paramCount; idx++) {
		result += this->paramTypes.at(idx);
		if (idx + 1 < this->paramCount) {
			result += "-";
		}
	}

	if (!result.empty()) {
		result = "@" + generateParamSymbol(result);
	}

	std::string ns = this->getNamespace();
	if (!ns.empty() && !ns.ends_with("::")) {
		ns = ns + "::";
	}

	return "(" + this->returnTypeId + ")" + ns + this->getName() + result;
}

std::string FunctionDescriptor::generateParamSymbol(std::string paramStr) {

	std::array<uint64_t, 2> hashOut = { 0, 0 };
	int seed = 0;

	MurmurHash3_x64_128(paramStr.data(), static_cast<int>(paramStr.size()), seed, hashOut.data());
	std::array<uint8_t, 16> bytes;
	std::memcpy(bytes.data(), hashOut.data(), 16);

	return llvm::encodeBase64(bytes);

}

FunctionDescriptor* FunctionDescriptor::copy() {
	return new FunctionDescriptor(
		this->name, 
		this->nsName, 
		this->returnTypeId, 
		this->paramCount,
		this->paramTypes,
		this->callback
	);
}

DescriptorType FunctionDescriptor::getType() {
	return DescriptorType::FUNCTION;
}

std::string FunctionDescriptor::getTypeStr() {
	return "FUNCTION";
}

llvm::json::Object FunctionDescriptor::getJson() {

	return llvm::json::Object{
		{ "id", this->getId() },
		{ "namespace", this->getNamespace() },
		{ "name", this->getName() },
		{ "const", this->isConst },
		{ "volatile", this->isVolatile },
		{ "type", this->getTypeStr() },
		{ "returnTypeId", this->returnTypeId },
		{ "paramCount", this->paramCount },
		{ "paramTypeIds", llvm::json::Array(this->paramTypes) },
		{ "callback", this->callback }
	};

}

RecordTypeDescriptor::RecordTypeDescriptor(
	std::string name, 
	std::string namespacePath,
	bool isConst, 
	bool isVoliate,
	bool movable, 
	bool hasNoArgsCtor,
	bool completeType
) : TypeDescriptor(name, namespacePath) {
	
	this->isConst = isConst;
	this->isVolatile = isVoliate;
	this->movable = movable;
	this->hasNoArgCtor = hasNoArgsCtor;
	this->completeType = completeType;

}

DescriptorType RecordTypeDescriptor::getType() {
	return DescriptorType::RECORD;
}

void RecordTypeDescriptor::updateState(DescriptorState state) {
	this->state = state;
}

DescriptorState RecordTypeDescriptor::getState() {
	return this->state;
}

std::string RecordTypeDescriptor::getTypeStr() {
	return "RECORD";
}

RecordTypeDescriptor* RecordTypeDescriptor::copy() {
	
	RecordTypeDescriptor* copied = new RecordTypeDescriptor(
		this->getName(), this->getNamespace(),
		this->isConst, this->isVolatile,
		this->movable,this->hasNoArgCtor,
		this->completeType
	);

	copied->fields = this->fields;
	copied->publicMethods = this->publicMethods;
	copied->overrideableMethods = this->overrideableMethods;
	copied->state = this->state;
	return copied;

}


void RecordTypeDescriptor::updateField(std::string fieldName, std::string typeId) {
	if (fieldName.empty() || typeId.empty()) {
		return;
	}
	this->fields.insert_or_assign(fieldName, typeId);
}

llvm::json::Object RecordTypeDescriptor::getJson() {

	llvm::json::Object fields;
	for (auto& [key, val] : this->fields) {
		fields[key] = val;
	}

	return llvm::json::Object{
		{ "id", this->getId() },
		{ "namespace", this->getNamespace() },
		{ "name", this->getName() },
		{ "const", this->isConst },
		{ "volatile", this->isVolatile },
		{ "type", this->getTypeStr() },
		{ "fields", std::move(fields) },
		{ "movable", this->movable },
		{ "hasNoArgsCtor", this->hasNoArgCtor },
		{ "completeType", this->completeType }
	};

}

PointerTypeDescriptor::PointerTypeDescriptor(
	std::string targetTypeId,
	std::string targetTypeName,
	bool isConst, 
	bool isVolatile
) : TypeDescriptor(targetTypeName, "") {
	this->targetTypeId = targetTypeId;
	this->isConst = isConst;
	this->isVolatile = isVolatile;
}

DescriptorType PointerTypeDescriptor::getType() {
	return DescriptorType::POINTER;
}

std::string PointerTypeDescriptor::getTypeStr() {
	return "POINTER";
}

std::string PointerTypeDescriptor::getName() {
	return "*" + this->targetTypeId;
}

PointerTypeDescriptor* PointerTypeDescriptor::copy() {
	return new PointerTypeDescriptor(this->targetTypeId, this->getName(), this->isConst, this->isVolatile);
}

std::string PointerTypeDescriptor::getId() {
	std::string cv = "";
	if (this->isConst && this->isVolatile) {
		cv = "[CV]";
	}
	else if (this->isVolatile) {
		cv = "[V]";
	}
	else if (this->isConst) {
		cv = "[C]";
	}
	return cv + "*" + this->targetTypeId;
}

llvm::json::Object PointerTypeDescriptor::getJson() {

	return llvm::json::Object{
		{ "id", this->getId() },
		{ "namespace", this->getNamespace() },
		{ "name", this->getName() },
		{ "const", this->isConst },
		{ "volatile", this->isVolatile },
		{ "type", this->getTypeStr() },
		{ "targetId", this->targetTypeId }
	};

}