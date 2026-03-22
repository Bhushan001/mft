export interface MappingRuleResponse {
  ruleId: string;
  tenantId: string;
  name: string;
  description?: string;
  version: number;
  active: boolean;
  ruleDefinition: string;
  sourceSystem?: string;
  targetSystem?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMappingRuleRequest {
  name: string;
  description?: string;
  ruleDefinition: string;
  sourceSystem?: string;
  targetSystem?: string;
}

export interface UpdateMappingRuleRequest {
  name?: string;
  description?: string;
  ruleDefinition?: string;
  sourceSystem?: string;
  targetSystem?: string;
}
