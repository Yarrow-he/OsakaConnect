# 验证用，勿合并：故意失败，用于确认必过检查 contract 能拦截合并。
def test_intentional_failure_for_branch_protection():
    assert False, "intentional failure: verify required check blocks merge"
