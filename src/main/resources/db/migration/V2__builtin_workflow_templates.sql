-- Tenant "sistema": no representa un cliente real, sirve solo como dueño de
-- las plantillas de workflow built-in que se clonan al registrar un tenant
-- nuevo (esa lógica de clonado corresponde a una fase posterior).
INSERT INTO tenant (name, subscription_type)
VALUES ('__system_templates__', 'ENTERPRISE');

-- Plantilla built-in "Scrum": tipos de work item, estados, transiciones y
-- estado inicial por tipo, expresados en JSONB para que el dominio de
-- workflow los interprete sin necesitar más migraciones por cada plantilla.
INSERT INTO workflow_definition (tenant_id, name, is_builtin_template, states_and_transitions)
SELECT
    t.id,
    'Scrum',
    TRUE,
    '{
      "version": 1,
      "itemTypes": [
        { "key": "EPIC",   "name": "Epic",   "allowedChildTypes": ["STORY"] },
        { "key": "STORY",  "name": "Story",  "allowedChildTypes": ["TASK"] },
        { "key": "TASK",   "name": "Task",   "allowedChildTypes": [] },
        { "key": "SPRINT", "name": "Sprint", "allowedChildTypes": [] }
      ],
      "states": [
        { "key": "BACKLOG",     "name": "Backlog",     "category": "TODO",        "order": 0, "appliesTo": ["EPIC", "STORY"] },
        { "key": "TODO",        "name": "To Do",       "category": "TODO",        "order": 1, "appliesTo": ["EPIC", "STORY"] },
        { "key": "DOING",       "name": "Doing",       "category": "IN_PROGRESS", "order": 2, "appliesTo": ["EPIC", "STORY"] },
        { "key": "TESTING",     "name": "Testing",     "category": "IN_PROGRESS", "order": 3, "appliesTo": ["EPIC", "STORY"] },
        { "key": "DONE",        "name": "Done",        "category": "DONE",        "order": 4, "appliesTo": ["EPIC", "STORY", "TASK"] },
        { "key": "TO_DO",       "name": "To Do",       "category": "TODO",        "order": 0, "appliesTo": ["TASK"] },
        { "key": "IN_PROGRESS", "name": "In Progress", "category": "IN_PROGRESS", "order": 1, "appliesTo": ["TASK"] },
        { "key": "CODE_REVIEW", "name": "Code Review", "category": "IN_PROGRESS", "order": 2, "appliesTo": ["TASK"] },
        { "key": "QA",          "name": "QA",          "category": "IN_PROGRESS", "order": 3, "appliesTo": ["TASK"] },
        { "key": "BLOCKED",     "name": "Blocked",     "category": "BLOCKED",     "order": 4, "appliesTo": ["TASK"] }
      ],
      "transitions": [
        { "from": "BACKLOG", "to": "TODO", "appliesTo": ["EPIC", "STORY"] },
        { "from": "TODO", "to": "DOING", "appliesTo": ["EPIC", "STORY"] },
        { "from": "DOING", "to": "TESTING", "appliesTo": ["EPIC", "STORY"] },
        { "from": "TESTING", "to": "DONE", "appliesTo": ["EPIC", "STORY"] },
        { "from": "TESTING", "to": "DOING", "appliesTo": ["EPIC", "STORY"] },
        { "from": "DOING", "to": "TODO", "appliesTo": ["EPIC", "STORY"] },
        { "from": "TO_DO", "to": "IN_PROGRESS", "appliesTo": ["TASK"] },
        { "from": "IN_PROGRESS", "to": "CODE_REVIEW", "appliesTo": ["TASK"] },
        { "from": "IN_PROGRESS", "to": "BLOCKED", "appliesTo": ["TASK"] },
        { "from": "BLOCKED", "to": "IN_PROGRESS", "appliesTo": ["TASK"] },
        { "from": "CODE_REVIEW", "to": "QA", "appliesTo": ["TASK"] },
        { "from": "CODE_REVIEW", "to": "IN_PROGRESS", "appliesTo": ["TASK"] },
        { "from": "QA", "to": "DONE", "appliesTo": ["TASK"] },
        { "from": "QA", "to": "IN_PROGRESS", "appliesTo": ["TASK"] }
      ],
      "initialStateByType": { "EPIC": "BACKLOG", "STORY": "BACKLOG", "TASK": "TO_DO", "SPRINT": "TODO" }
    }'::jsonb
FROM tenant t
WHERE t.name = '__system_templates__';
